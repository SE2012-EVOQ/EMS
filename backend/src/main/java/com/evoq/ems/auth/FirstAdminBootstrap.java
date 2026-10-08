package com.evoq.ems.auth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Opt-in operator bootstrap only; no web registration or embedded credentials. */
@Component
@Profile("!dev")
@Order(10)
@ConditionalOnProperty(name = "ems.bootstrap.enabled", havingValue = "true")
public class FirstAdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final String username, passwordFile;
    private final FirstRunSetupService setup;
    private final Long employeeId;
    public FirstAdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder, PlatformTransactionManager manager,
            @Value("${ems.bootstrap.username:}") String username,
            @Value("${ems.bootstrap.employee-id:0}") Long employeeId,
            @Value("${ems.bootstrap.password-file:}") String passwordFile, FirstRunSetupService setup) {
        this.jdbc = jdbc; this.encoder = encoder; this.username = username; this.employeeId = employeeId;
        this.passwordFile = passwordFile; this.transaction = new TransactionTemplate(manager);
        this.setup = setup;
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (username == null || !username.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,99}") || employeeId == null || employeeId <= 0 || passwordFile.isBlank())
            throw new IllegalStateException("Bootstrap requires a valid username, existing employee ID and external password file");
        Path file = Path.of(passwordFile);
        if (!Files.isRegularFile(file) || Files.size(file) > 128) throw new IllegalStateException("Bootstrap password file is missing or too large");
        String password = Files.readString(file, StandardCharsets.UTF_8).strip();
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < 6 || bytes > 72) throw new IllegalStateException("Bootstrap password must contain at least 6 characters and at most 72 UTF-8 bytes");
        transaction.executeWithoutResult(status -> {
            setup.requireOpen();
            // Share the setup marker lock, then verify the existing role and employee.
            var roles = jdbc.queryForList("SELECT role_id FROM role WHERE name = 'MANAGER_ADMIN' FOR UPDATE", Long.class);
            if (roles.size() != 1) throw new IllegalStateException("Seed runtime roles before bootstrap");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Long.class) != 0)
                throw new IllegalStateException("Bootstrap is only for a fresh install with no accounts; disable it after first use");
            var employees = jdbc.queryForList("SELECT employee_id FROM employee WHERE employee_id = ? AND status = 'ACTIVE' FOR UPDATE", Long.class, employeeId);
            if (employees.size() != 1) throw new IllegalStateException("Bootstrap employee must already exist and be active");
            jdbc.update("INSERT INTO user_account (employee_id, role_id, username, password_hash, active) VALUES (?, ?, ?, ?, TRUE)",
                    employeeId, roles.getFirst(), username, encoder.encode(password));
            setup.markCompleted();
        });
    }
}
