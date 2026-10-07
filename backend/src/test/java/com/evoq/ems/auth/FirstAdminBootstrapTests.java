package com.evoq.ems.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class FirstAdminBootstrapTests {
    @TempDir Path temp;
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    Path secret;
    @BeforeEach void setup() throws Exception {
        secret = temp.resolve("secret"); Files.writeString(secret, "test-only-long-bootstrap-password\n");
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(jdbc.queryForList("SELECT role_id FROM role WHERE name = 'MANAGER_ADMIN' FOR UPDATE", Long.class)).thenReturn(List.of(1L));
        when(jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Long.class)).thenReturn(0L);
        when(jdbc.queryForList("SELECT employee_id FROM employee WHERE employee_id = ? AND status = 'ACTIVE' FOR UPDATE", Long.class, 3L)).thenReturn(List.of(3L));
    }
    FirstAdminBootstrap bootstrap(String name, Long employeeId, String path) {
        return new FirstAdminBootstrap(jdbc, encoder, manager, name, employeeId, path);
    }
    @Test void optInCreatesOnlyHashedAccountForExistingActiveEmployee() throws Exception {
        bootstrap("first.admin", 3L, secret.toString()).run(new DefaultApplicationArguments());
        verify(jdbc).update(eq("INSERT INTO user_account (employee_id, role_id, username, password_hash, active) VALUES (?, ?, ?, ?, TRUE)"),
            eq(3L), eq(1L), eq("first.admin"), argThat((Object hash) -> encoder.matches("test-only-long-bootstrap-password", hash.toString())));
        verify(manager).commit(any());
    }
    @Test void existingAccountsRefuseBootstrapAndRollback() {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Long.class)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,secret.toString()).run(new DefaultApplicationArguments()));
        verify(manager).rollback(any()); verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void inactiveOrMissingEmployeeRefusesAndRollsBack() {
        when(jdbc.queryForList("SELECT employee_id FROM employee WHERE employee_id = ? AND status = 'ACTIVE' FOR UPDATE", Long.class, 3L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,secret.toString()).run(new DefaultApplicationArguments()));
        verify(manager).rollback(any());
    }
    @Test void missingRoleRefusesAndRollsBack() {
        when(jdbc.queryForList("SELECT role_id FROM role WHERE name = 'MANAGER_ADMIN' FOR UPDATE", Long.class)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,secret.toString()).run(new DefaultApplicationArguments()));
        verify(manager).rollback(any());
    }
    @Test void invalidInputsAndMissingFileRefuseBeforeDatabaseAccess() {
        assertThrows(IllegalStateException.class, () -> bootstrap("",3L,secret.toString()).run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",0L,secret.toString()).run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,temp.resolve("missing").toString()).run(new DefaultApplicationArguments()));
        verifyNoInteractions(jdbc);
    }
    @Test void ShortAndOversizedPasswordsRefuseBeforeDatabaseAccess() throws Exception {
        Files.writeString(secret,"short");
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,secret.toString()).run(new DefaultApplicationArguments()));
        Files.writeString(secret,"x".repeat(73));
        assertThrows(IllegalStateException.class, () -> bootstrap("first.admin",3L,secret.toString()).run(new DefaultApplicationArguments()));
        verifyNoInteractions(jdbc);
    }
}
