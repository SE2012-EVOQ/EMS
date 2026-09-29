package com.evoq.ems.auth;

public record AuthUserResponse(Long userId, Long employeeId, String username, String role) {

    public static AuthUserResponse from(AccountPrincipal principal) {
        return new AuthUserResponse(
                principal.getUserId(), principal.getEmployeeId(), principal.getUsername(), principal.getRole());
    }
}
