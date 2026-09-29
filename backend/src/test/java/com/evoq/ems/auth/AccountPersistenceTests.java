package com.evoq.ems.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class AccountPersistenceTests {

    @Test
    void userDetailsComeFromAccountAndRole() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccount account = mock(UserAccount.class);
        Role role = mock(Role.class);
        when(accounts.findByUsername("test.supervisor")).thenReturn(Optional.of(account));
        when(account.getId()).thenReturn(2L);
        when(account.getEmployeeId()).thenReturn(2L);
        when(account.getUsername()).thenReturn("test.supervisor");
        when(account.getRole()).thenReturn(role);
        when(role.getName()).thenReturn("SUPERVISOR");
        when(account.isActive()).thenReturn(false);

        AccountPrincipal principal = (AccountPrincipal) new DatabaseUserDetailsService(accounts)
                .loadUserByUsername("test.supervisor");

        assertEquals(2L, principal.getUserId());
        assertEquals(2L, principal.getEmployeeId());
        assertEquals("SUPERVISOR", principal.getRole());
        assertEquals("ROLE_SUPERVISOR", principal.getAuthorities().iterator().next().getAuthority());
        assertFalse(principal.isEnabled());
    }
}
