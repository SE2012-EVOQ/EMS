package com.evoq.ems.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import com.evoq.ems.attendance.service.AttendanceReconciliationJob;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Owns a disposable schema; never clears the developer's database or creates their first admin. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("setup-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FirstRunSetupMySqlTests {
    static final String schema = "ems_first_run_test_" + UUID.randomUUID().toString().replace("-", "");
    static DriverManagerDataSource server;
    static DriverManagerDataSource fixture;
    static String migration;
    @Autowired JdbcTemplate jdbc;
    @Autowired FirstRunSetupService setup;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoSpyBean PasswordEncoder encoder;
    @MockitoBean AttendanceReconciliationJob job;

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) throws Exception {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/evoq_ems?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String serverUrl = url.replaceFirst("(/)[^/?]+(\\?|$)", "$1$2");
        String testUrl = serverUrl.replaceFirst("/(\\?|$)", "/" + schema + "$1");
        String username = System.getenv().getOrDefault("DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        server = new DriverManagerDataSource(serverUrl, username, password);
        new JdbcTemplate(server).execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        fixture = new DriverManagerDataSource(testUrl, username, password);
        String frozen = Files.readString(Path.of("../database/01_schema.sql"));
        migration = Files.readString(Path.of("../database/07_first_run_setup.sql"));
        String sql = frozen.substring(frozen.indexOf("CREATE TABLE role")) + "\n" + Files.readString(Path.of("../database/06_runtime_roles.sql")) + "\n" + migration;
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(fixture);
        properties.add("spring.datasource.url", () -> testUrl);
        properties.add("spring.datasource.username", () -> username);
        properties.add("spring.datasource.password", () -> password);
    }

    @AfterAll static void dropOwnedDatabase() {
        if (server != null) new JdbcTemplate(server).execute("DROP DATABASE `" + schema + "`");
    }
    @BeforeEach void freshInstallation() {
        jdbc.update("DELETE FROM user_account"); jdbc.update("DELETE FROM employee"); jdbc.update("DELETE FROM department");
        jdbc.update("INSERT INTO first_run_setup(setup_id,completed) VALUES(1,FALSE) ON DUPLICATE KEY UPDATE completed=FALSE,completed_at=NULL");
    }
    FirstRunSetupRequest details(String suffix) {
        return new FirstRunSetupRequest("First", "Administrator", "first" + suffix + "@example.invalid", java.time.LocalDate.of(2026,1,1),
                "Administration", "Administrator", "first.admin" + suffix, "TestOnlyLongPassword123!");
    }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    void assertEmpty() { assertEquals(0, count("department")); assertEquals(0,count("employee")); assertEquals(0,count("user_account")); assertTrue(setup.required()); }

    @Test void freshStatusIsPublicButCreationRequiresCsrfAndValidFields() throws Exception {
        mvc.perform(get("/api/auth/setup")).andExpect(status().isOk()).andExpect(jsonPath("$.required").value(true));
        mvc.perform(post("/api/auth/setup").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(details(""))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists()).andExpect(jsonPath("$.fieldErrors.password").exists());
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        assertEmpty();
    }
    @Test void createsOneHashedManagerAndSupportsNormalLogin() throws Exception {
        mvc.perform(post("/api/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(details(""))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.required").value(false)).andExpect(jsonPath("$.password").doesNotExist());
        assertEquals(1,count("department")); assertEquals(1,count("employee")); assertEquals(1,count("user_account"));
        assertEquals("MANAGER_ADMIN",jdbc.queryForObject("SELECT r.name FROM user_account u JOIN role r ON r.role_id=u.role_id",String.class));
        assertTrue(encoder.matches(details("").password(),jdbc.queryForObject("SELECT password_hash FROM user_account",String.class)));
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM employee",String.class));
        assertFalse(setup.required());
        mvc.perform(post("/api/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(details("2"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username",details("").username()).param("password",details("").password()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MANAGER_ADMIN"));
        assertEquals(1,count("user_account"));
    }
    @Test void persistenceFailureRollsBackDepartmentEmployeeAccountAndMarker() {
        doThrow(new IllegalStateException("Injected encoding failure")).when(encoder).encode(details("").password());
        assertThrows(IllegalStateException.class, () -> setup.create(details("")));
        assertEmpty();
        doCallRealMethod().when(encoder).encode(details("").password());
        setup.create(details("")); assertFalse(setup.required());
    }
    @Test void competingSubmissionsProduceOneAdminAndOneConflict() throws Exception {
        var start = new CountDownLatch(1); var workers=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> one = () -> { start.await(); try { setup.create(details("1")); return 201; } catch (ResponseStatusException error) { return error.getStatusCode().value(); } };
            Callable<Integer> two = () -> { start.await(); try { setup.create(details("2")); return 201; } catch (ResponseStatusException error) { return error.getStatusCode().value(); } };
            var first=workers.submit(one); var second=workers.submit(two); start.countDown();
            assertEquals(List.of(201,409),List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS)).stream().sorted().toList());
            assertEquals(1,count("department")); assertEquals(1,count("employee")); assertEquals(1,count("user_account")); assertFalse(setup.required());
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(15,TimeUnit.SECONDS)); }
    }
    @Test void completedMarkerSurvivesDeactivationDemotionDeletionAndMigrationRerun() {
        setup.create(details(""));
        jdbc.update("UPDATE user_account SET active=FALSE,role_id=(SELECT role_id FROM role WHERE name='EMPLOYEE')");
        jdbc.update("UPDATE employee SET status='INACTIVE'"); assertFalse(setup.required());
        jdbc.update("DELETE FROM user_account"); jdbc.update("DELETE FROM employee"); jdbc.update("DELETE FROM department");
        new ResourceDatabasePopulator(new ByteArrayResource(migration.getBytes(StandardCharsets.UTF_8))).execute(fixture);
        assertFalse(setup.required());
        assertEquals(409,assertThrows(ResponseStatusException.class, () -> setup.create(details("2"))).getStatusCode().value());
        assertEquals(0,count("user_account"));
    }
    @Test void existingInactiveOrdinaryAccountCannotReopenSetupAndMigrationSealsIt() {
        setup.create(details(""));
        jdbc.update("UPDATE user_account SET active=FALSE,role_id=(SELECT role_id FROM role WHERE name='EMPLOYEE')");
        jdbc.update("DELETE FROM first_run_setup");
        new ResourceDatabasePopulator(new ByteArrayResource(migration.getBytes(StandardCharsets.UTF_8))).execute(fixture);
        assertFalse(setup.required());
        assertEquals(Boolean.TRUE,jdbc.queryForObject("SELECT completed FROM first_run_setup",Boolean.class));
        assertThrows(ResponseStatusException.class, () -> setup.create(details("2")));
    }
    @Test void startupSealsLegacyProvisioningAndMissingMarkerFailsClosed() {
        setup.create(details("")); jdbc.update("UPDATE first_run_setup SET completed=FALSE,completed_at=NULL");
        assertFalse(setup.required()); setup.sealExistingInstallation();
        assertEquals(Boolean.TRUE,jdbc.queryForObject("SELECT completed FROM first_run_setup",Boolean.class));
        jdbc.update("DELETE FROM first_run_setup");
        assertThrows(IllegalStateException.class,setup::required);
        assertThrows(IllegalStateException.class, () -> setup.create(details("2")));
        assertEquals(1,count("user_account"));
    }
    @Test void multibytePasswordCannotExceedBcryptByteLimitAndDuplicateEmailRollsBack() throws Exception {
        var original=details("");
        var bytes=new FirstRunSetupRequest(original.firstName(), original.lastName(),original.email(),original.hireDate(),original.departmentName(),original.jobTitle(),original.username(),"é".repeat(40));
        mvc.perform(post("/api/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bytes)))
                .andExpect(status().isBadRequest()); assertEmpty();
        jdbc.update("INSERT INTO department(name) VALUES('Existing')");
        jdbc.update("INSERT INTO employee(department_id,first_name,last_name,email,hire_date,job_title,status) VALUES ((SELECT department_id FROM department),'Existing','Person',?,'2026-01-01','Engineer','ACTIVE')",original.email());
        assertThrows(ResponseStatusException.class, () -> setup.create(original));
        assertTrue(setup.required()); assertEquals(1,count("employee")); assertEquals(1,count("department")); assertEquals(0,count("user_account"));
    }
}
