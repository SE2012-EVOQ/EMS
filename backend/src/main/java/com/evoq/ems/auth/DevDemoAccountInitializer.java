package com.evoq.ems.auth;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("dev")
public class DevDemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDemoAccountInitializer.class);

    private static final List<DepartmentSeed> DEPARTMENTS = List.of(
            new DepartmentSeed("Management", "Executive and operational management"),
            new DepartmentSeed("Engineering", "Product engineering and research"),
            new DepartmentSeed("Development Access", "Local development login accounts only")
    );

    private static final List<TeamSeed> TEAMS = List.of(
            new TeamSeed("Management \u00b7 Operations", "Operations and executive leadership"),
            new TeamSeed("Vision \u00b7 Vision Edge", "Computer vision and edge machine learning"),
            new TeamSeed("Platform \u00b7 EMS Portal", "Core enterprise management platform engineering"),
            new TeamSeed("Development Scheduling Team", "Reserved for local attendance and scheduling development")
    );

    private static final List<OfficialEmployeeSeed> OFFICIAL_EMPLOYEES = List.of(
            new OfficialEmployeeSeed("A.", "Perera", "a.perera@evoq.ai", "+94 77 245 1001", "Colombo 05",
                    "2024-02-05", "Director / Manager", "Management", "Management \u00b7 Operations", null, "a.perera", "MANAGER_ADMIN"),
            new OfficialEmployeeSeed("Sarah", "Fernando", "sarah@evoq.ai", "+94 71 880 2240", "Rajagiriya",
                    "2024-05-12", "Computer Vision Lead", "Engineering", "Vision \u00b7 Vision Edge", "a.perera@evoq.ai", "sarah", "SUPERVISOR"),
            new OfficialEmployeeSeed("Ravin", "Silva", "ravin@evoq.ai", "+94 76 312 6638", "Nugegoda",
                    "2025-01-15", "ML Engineer", "Engineering", "Vision \u00b7 Vision Edge", "sarah@evoq.ai", "ravin", "EMPLOYEE"),
            new OfficialEmployeeSeed("Maya", "de Silva", "maya@evoq.ai", "+94 77 555 4321", "Colombo 03",
                    "2024-08-01", "Platform Team Lead", "Engineering", "Platform \u00b7 EMS Portal", "a.perera@evoq.ai", "maya", "SUPERVISOR"),
            new OfficialEmployeeSeed("Nadeem", "Hassan", "nadeem@evoq.ai", "+94 77 900 3172", "Dehiwala",
                    "2025-03-03", "Software Engineer", "Engineering", "Platform \u00b7 EMS Portal", "maya@evoq.ai", "nadeem", "EMPLOYEE"),
            new OfficialEmployeeSeed("Ishara", "Jayasinghe", "ishara@evoq.ai", "+94 70 432 9090", "Kottawa",
                    "2025-03-21", "CV Engineer", "Engineering", "Vision \u00b7 Vision Edge", "sarah@evoq.ai", "ishara", "EMPLOYEE"),
            new OfficialEmployeeSeed("Kavindu", "Peris", "kavindu@evoq.ai", "+94 71 333 8899", "Moratuwa",
                    "2025-04-10", "Frontend Engineer", "Engineering", "Platform \u00b7 EMS Portal", "maya@evoq.ai", "kavindu", "EMPLOYEE"),
            new OfficialEmployeeSeed("Dinithi", "Wickramasinghe", "dinithi@evoq.ai", "+94 77 444 1122", "Mount Lavinia",
                    "2025-06-01", "QA Engineer", "Engineering", "Platform \u00b7 EMS Portal", "maya@evoq.ai", "dinithi", "EMPLOYEE"),
            new OfficialEmployeeSeed("Shenal", "Cooray", "shenal@evoq.ai", "+94 76 888 7766", "Kelaniya",
                    "2025-07-15", "Research Associate", "Engineering", "Vision \u00b7 Vision Edge", "sarah@evoq.ai", "shenal", "EMPLOYEE"),
            new OfficialEmployeeSeed("Anuki", "Dias", "anuki@evoq.ai", "+94 70 999 5544", "Battaramulla",
                    "2025-09-01", "Product Specialist", "Management", "Management \u00b7 Operations", "a.perera@evoq.ai", "anuki", "EMPLOYEE")
    );

    private static final List<DemoAccount> LEGACY_DEMO_ACCOUNTS = List.of(
            new DemoAccount("demo.manager", "Manager", "MANAGER_ADMIN"),
            new DemoAccount("demo.supervisor", "Supervisor", "SUPERVISOR"),
            new DemoAccount("demo.employee", "Employee", "EMPLOYEE")
    );

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final String password;

    public DevDemoAccountInitializer(JdbcTemplate jdbc, PasswordEncoder encoder,
            PlatformTransactionManager transactionManager,
            @Value("${DEV_DEMO_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.transaction = new TransactionTemplate(transactionManager);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("DEV_DEMO_PASSWORD must contain at least 8 characters in the dev profile");
        }

        transaction.executeWithoutResult(status -> {
            // 1. Seed base roles if missing
            ensureRoles();

            // 2. Seed departments
            for (DepartmentSeed d : DEPARTMENTS) {
                ensureDepartment(d.name(), d.description());
            }

            // 3. Seed teams
            for (TeamSeed t : TEAMS) {
                ensureTeam(t.name(), t.description());
            }

            // 4. Seed official directory employees
            for (OfficialEmployeeSeed e : OFFICIAL_EMPLOYEES) {
                long deptId = getDepartmentId(e.department());
                Long teamId = e.team() != null ? getTeamId(e.team()) : null;
                long empId = ensureOfficialEmployee(deptId, teamId, e);
                long roleId = requiredRole(e.role());
                ensureAccount(empId, roleId, e.username());
            }

            // 5. Link reporting supervisors for official employees
            for (OfficialEmployeeSeed e : OFFICIAL_EMPLOYEES) {
                if (e.supervisorEmail() != null) {
                    Long supervisorId = getEmployeeIdByEmail(e.supervisorEmail());
                    if (supervisorId != null) {
                        jdbc.update("UPDATE employee SET supervisor_id = ? WHERE email = ?",
                                supervisorId, e.email());
                    }
                }
            }

            // 6. Seed legacy demo accounts
            long devDeptId = getDepartmentId("Development Access");
            Long devTeamId = getTeamId("Development Scheduling Team");
            for (DemoAccount account : LEGACY_DEMO_ACCOUNTS) {
                long roleId = requiredRole(account.role());
                long employeeId = ensureLegacyDemoEmployee(devDeptId, devTeamId, account);
                ensureAccount(employeeId, roleId, account.username());
            }
        });

        log.info("Official demo employees and development accounts are initialized successfully");
    }

    private void ensureRoles() {
        String[][] roles = {
                {"EMPLOYEE", "Standard employee access"},
                {"SUPERVISOR", "Employee access plus team supervision functions"},
                {"MANAGER_ADMIN", "Employee access plus organization-wide administration functions"}
        };
        for (String[] r : roles) {
            List<Long> ids = jdbc.queryForList("SELECT role_id FROM role WHERE name = ?", Long.class, r[0]);
            if (ids.isEmpty()) {
                jdbc.update("INSERT INTO role (name, description) VALUES (?, ?)", r[0], r[1]);
            }
        }
    }

    private long ensureDepartment(String name, String description) {
        List<Long> ids = jdbc.queryForList("SELECT department_id FROM department WHERE name = ?", Long.class, name);
        if (!ids.isEmpty()) return ids.getFirst();

        jdbc.update("INSERT INTO department (name, description) VALUES (?, ?)", name, description);
        return jdbc.queryForObject("SELECT department_id FROM department WHERE name = ?", Long.class, name);
    }

    private long getDepartmentId(String name) {
        return jdbc.queryForObject("SELECT department_id FROM department WHERE name = ?", Long.class, name);
    }

    private long ensureTeam(String name, String description) {
        List<Long> ids = jdbc.queryForList("SELECT team_id FROM team_project WHERE name = ?", Long.class, name);
        if (!ids.isEmpty()) return ids.getFirst();

        jdbc.update("INSERT INTO team_project (name, description) VALUES (?, ?)", name, description);
        return jdbc.queryForObject("SELECT team_id FROM team_project WHERE name = ?", Long.class, name);
    }

    private Long getTeamId(String name) {
        List<Long> ids = jdbc.queryForList("SELECT team_id FROM team_project WHERE name = ?", Long.class, name);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long getEmployeeIdByEmail(String email) {
        List<Long> ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, email);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private long requiredRole(String name) {
        List<Long> ids = jdbc.queryForList("SELECT role_id FROM role WHERE name = ?", Long.class, name);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Required role " + name + " is missing; run database/02_sample_data.sql");
        }
        return ids.getFirst();
    }

    private long ensureOfficialEmployee(long deptId, Long teamId, OfficialEmployeeSeed e) {
        List<Long> ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, e.email());
        if (ids.isEmpty()) {
            jdbc.update("""
                    INSERT INTO employee
                    (department_id, team_id, first_name, last_name, email, phone, address, hire_date, job_title, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """,
                    deptId, teamId, e.firstName(), e.lastName(), e.email(),
                    e.phone(), e.address(), Date.valueOf(LocalDate.parse(e.hireDate())), e.jobTitle());
            ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, e.email());
        } else {
            jdbc.update("""
                    UPDATE employee
                    SET department_id = ?, team_id = ?, first_name = ?, last_name = ?, phone = ?, address = ?, job_title = ?, status = 'ACTIVE'
                    WHERE employee_id = ?
                    """,
                    deptId, teamId, e.firstName(), e.lastName(), e.phone(), e.address(), e.jobTitle(), ids.getFirst());
        }
        return ids.getFirst();
    }

    private long ensureLegacyDemoEmployee(long departmentId, Long teamId, DemoAccount account) {
        String email = account.username() + "@evoq.invalid";
        List<Long> ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, email);
        if (ids.isEmpty()) {
            jdbc.update("""
                    INSERT INTO employee
                    (department_id, team_id, first_name, last_name, email, hire_date, job_title, status)
                    VALUES (?, ?, 'Demo', ?, ?, ?, 'Development access', 'ACTIVE')
                    """, departmentId, teamId, account.lastName(), email, Date.valueOf(LocalDate.of(2026, 1, 1)));
            ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, email);
        }
        return ids.getFirst();
    }

    private void ensureAccount(long employeeId, long roleId, String username) {
        List<ExistingAccount> existing = jdbc.query("""
                SELECT employee_id, role_id, password_hash, active
                FROM user_account WHERE username = ?
                """, (rs, row) -> new ExistingAccount(
                rs.getLong("employee_id"), rs.getLong("role_id"),
                rs.getString("password_hash"), rs.getBoolean("active")), username);

        if (existing.isEmpty()) {
            jdbc.update("""
                    INSERT INTO user_account (employee_id, role_id, username, password_hash, active)
                    VALUES (?, ?, ?, ?, TRUE)
                    """, employeeId, roleId, username, encoder.encode(password));
            return;
        }

        ExistingAccount current = existing.getFirst();
        if (current.employeeId() != employeeId) {
            jdbc.update("DELETE FROM user_account WHERE username = ?", username);
            jdbc.update("""
                    INSERT INTO user_account (employee_id, role_id, username, password_hash, active)
                    VALUES (?, ?, ?, ?, TRUE)
                    """, employeeId, roleId, username, encoder.encode(password));
            return;
        }
        if (current.roleId() != roleId || !current.active() || !encoder.matches(password, current.passwordHash())) {
            jdbc.update("""
                    UPDATE user_account SET role_id = ?, password_hash = ?, active = TRUE
                    WHERE username = ?
                    """, roleId, encoder.encode(password), username);
        }
    }

    private record DepartmentSeed(String name, String description) {}
    private record TeamSeed(String name, String description) {}
    private record OfficialEmployeeSeed(
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            String hireDate,
            String jobTitle,
            String department,
            String team,
            String supervisorEmail,
            String username,
            String role
    ) {}
    private record DemoAccount(String username, String lastName, String role) {}
    private record ExistingAccount(long employeeId, long roleId, String passwordHash, boolean active) {}
}
