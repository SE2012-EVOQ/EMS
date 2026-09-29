package com.evoq.ems.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordServiceTests {

    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final PasswordService passwords = new PasswordService(accounts, encoder);

    @Test
    void checksCurrentPasswordAndStoresNewBcryptHash() {
        UserAccount account = mock(UserAccount.class);
        when(accounts.findByUsername("test.manager")).thenReturn(Optional.of(account));
        when(account.isActive()).thenReturn(true);
        when(account.getPasswordHash()).thenReturn(encoder.encode("old-password"));

        passwords.changePassword("test.manager", new ChangePasswordRequest("old-password", "new-password"));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(account).setPasswordHash(hash.capture());
        assertTrue(hash.getValue().startsWith("$2"));
        assertTrue(encoder.matches("new-password", hash.getValue()));
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        UserAccount account = mock(UserAccount.class);
        when(accounts.findByUsername("test.manager")).thenReturn(Optional.of(account));
        when(account.isActive()).thenReturn(true);
        when(account.getPasswordHash()).thenReturn(encoder.encode("old-password"));

        assertThrows(IllegalArgumentException.class, () -> passwords.changePassword(
                "test.manager", new ChangePasswordRequest("wrong-password", "new-password")));
        verify(account, never()).setPasswordHash(anyString());
    }
}
