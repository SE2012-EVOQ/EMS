package com.evoq.ems.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;

    public PasswordService(UserAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserAccount account = accounts.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account is inactive");
        }
        if (!encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (encoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different new password");
        }
        account.setPasswordHash(encoder.encode(request.newPassword()));
    }
}
