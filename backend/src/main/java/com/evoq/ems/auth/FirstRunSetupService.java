package com.evoq.ems.auth;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Statement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FirstRunSetupService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public FirstRunSetupService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public boolean required() {
        return !completed(false) && accountCount() == 0;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void create(FirstRunSetupRequest request) {
        requireOpen();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        var roles = jdbc.queryForList("SELECT role_id FROM role WHERE name = 'MANAGER_ADMIN' FOR UPDATE", Long.class);
        if (roles.size() != 1) throw new IllegalStateException("Required Manager/Admin role is missing");
        String email = request.email().strip();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE email = ?", Long.class, email) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An employee already uses this email");
        String department = request.departmentName().strip();
        var departments = jdbc.queryForList("SELECT department_id FROM department WHERE name = ?", Long.class, department);
        long departmentId = departments.isEmpty()
                ? insert("INSERT INTO department (name) VALUES (?)", department) : departments.getFirst();
        long employeeId = insert("""
                INSERT INTO employee (department_id, first_name, last_name, email, hire_date, job_title, status)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """, departmentId, request.firstName().strip(), request.lastName().strip(), email,
                Date.valueOf(request.hireDate()), request.jobTitle().strip());
        insert("""
                INSERT INTO user_account (employee_id, role_id, username, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, employeeId, roles.getFirst(), request.username(), encoder.encode(request.password()));
        markCompleted();
    }

    // Both browser setup and the optional operator bootstrap take this lock first.
    void requireOpen() {
        if (completed(true) || accountCount() != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "First administrator setup has already been completed. Sign in instead.");
    }

    void markCompleted() {
        jdbc.update("UPDATE first_run_setup SET completed = TRUE, completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP) WHERE setup_id = 1");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void sealExistingInstallation() {
        if (!completed(true) && accountCount() != 0) markCompleted();
    }

    private boolean completed(boolean lock) {
        List<Boolean> values = jdbc.queryForList("SELECT completed FROM first_run_setup WHERE setup_id = 1" + (lock ? " FOR UPDATE" : ""), Boolean.class);
        if (values.size() != 1) throw new IllegalStateException("Apply database/07_first_run_setup.sql before starting the application");
        return values.getFirst();
    }

    private long accountCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Long.class);
    }

    private long insert(String sql, Object... values) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }
}
