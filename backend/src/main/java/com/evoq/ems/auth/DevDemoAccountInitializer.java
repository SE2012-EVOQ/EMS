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
    private static final String DEPARTMENT = "Development Access";
    private static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("demo.manager", "Manager", "MANAGER_ADMIN"),
            new DemoAccount("demo.supervisor", "Supervisor", "SUPERVISOR"),
            new DemoAccount("demo.employee", "Employee", "EMPLOYEE"));

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
            long departmentId = ensureDepartment();
            for (DemoAccount account : ACCOUNTS) {
                long roleId = requiredRole(account.role());
                long employeeId = ensureEmployee(departmentId, account);
                ensureAccount(employeeId, roleId, account);
            }
        });
        log.info("Development demo accounts are ready");
    }

    private long ensureDepartment() {
        List<Long> ids = jdbc.queryForList("SELECT department_id FROM department WHERE name = ?",
                Long.class, DEPARTMENT);
        if (!ids.isEmpty()) return ids.getFirst();

        jdbc.update("INSERT INTO department (name, description) VALUES (?, ?)",
                DEPARTMENT, "Local development login accounts only");
        return jdbc.queryForObject("SELECT department_id FROM department WHERE name = ?",
                Long.class, DEPARTMENT);
    }

    private long requiredRole(String name) {
        List<Long> ids = jdbc.queryForList("SELECT role_id FROM role WHERE name = ?", Long.class, name);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Required role " + name + " is missing; run database/02_sample_data.sql");
        }
        return ids.getFirst();
    }

    private long ensureEmployee(long departmentId, DemoAccount account) {
        String email = account.username() + "@evoq.invalid";
        List<Long> ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, email);
        if (ids.isEmpty()) {
            jdbc.update("""
                    INSERT INTO employee
                    (department_id, first_name, last_name, email, hire_date, job_title, status)
                    VALUES (?, 'Demo', ?, ?, ?, 'Development access', 'ACTIVE')
                    """, departmentId, account.lastName(), email, Date.valueOf(LocalDate.of(2026, 1, 1)));
            ids = jdbc.queryForList("SELECT employee_id FROM employee WHERE email = ?", Long.class, email);
        }
        long employeeId = ids.getFirst();
        jdbc.update("UPDATE employee SET status = 'ACTIVE' WHERE employee_id = ? AND status <> 'ACTIVE'",
                employeeId);
        return employeeId;
    }

    private void ensureAccount(long employeeId, long roleId, DemoAccount account) {
        List<ExistingAccount> existing = jdbc.query("""
                SELECT employee_id, role_id, password_hash, active
                FROM user_account WHERE username = ?
                """, (rs, row) -> new ExistingAccount(
                rs.getLong("employee_id"), rs.getLong("role_id"),
                rs.getString("password_hash"), rs.getBoolean("active")), account.username());

        if (existing.isEmpty()) {
            jdbc.update("""
                    INSERT INTO user_account (employee_id, role_id, username, password_hash, active)
                    VALUES (?, ?, ?, ?, TRUE)
                    """, employeeId, roleId, account.username(), encoder.encode(password));
            return;
        }

        ExistingAccount current = existing.getFirst();
        if (current.employeeId() != employeeId) {
            throw new IllegalStateException("Reserved demo username is linked to another employee: "
                    + account.username());
        }
        if (current.roleId() != roleId || !current.active() || !encoder.matches(password, current.passwordHash())) {
            jdbc.update("""
                    UPDATE user_account SET role_id = ?, password_hash = ?, active = TRUE
                    WHERE username = ?
                    """, roleId, encoder.encode(password), account.username());
        }
    }

    private record DemoAccount(String username, String lastName, String role) {
    }

    private record ExistingAccount(long employeeId, long roleId, String passwordHash, boolean active) {
    }
}
