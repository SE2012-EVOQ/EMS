package com.evoq.ems.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountPersistenceTests {

    @Test
    void userDetailsComeFromAccountAndRole() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccount account = mock(UserAccount.class);
        Role role = mock(Role.class);
        when(accounts.findByUsername("dilan.perera")).thenReturn(Optional.of(account));
        when(account.getId()).thenReturn(2L);
        when(account.getEmployeeId()).thenReturn(2L);
        when(account.getUsername()).thenReturn("dilan.perera");
        when(account.getRole()).thenReturn(role);
        when(role.getName()).thenReturn("SUPERVISOR");
        when(account.isActive()).thenReturn(false);

        AccountPrincipal principal = (AccountPrincipal) new DatabaseUserDetailsService(accounts)
                .loadUserByUsername("dilan.perera");

        assertEquals(2L, principal.getUserId());
        assertEquals(2L, principal.getEmployeeId());
        assertEquals("SUPERVISOR", principal.getRole());
        assertEquals("ROLE_SUPERVISOR", principal.getAuthorities().iterator().next().getAuthority());
        assertFalse(principal.isEnabled());
    }

    @Test
    void devInitializerOnlyTargetsExactPlaceholderWithBcrypt() throws Exception {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        DemoPasswordInitializer initializer = new DemoPasswordInitializer();

        initializer.initializeDemoPasswords(accounts, encoder, "local-demo-password")
                .run(new DefaultApplicationArguments());

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(accounts).replacePlaceholderHashes(
                org.mockito.ArgumentMatchers.eq("DEMO_HASH_REPLACE_DURING_SETUP"), hash.capture());
        assertTrue(hash.getValue().startsWith("$2"));
        assertTrue(encoder.matches("local-demo-password", hash.getValue()));
    }

    @Test
    void missingDemoPasswordLeavesAccountsUntouched() throws Exception {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        DemoPasswordInitializer initializer = new DemoPasswordInitializer();
        initializer.initializeDemoPasswords(accounts, new BCryptPasswordEncoder(), "")
                .run(new DefaultApplicationArguments());
        verify(accounts, never()).replacePlaceholderHashes(anyString(), anyString());
    }
}
