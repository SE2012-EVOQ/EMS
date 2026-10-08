package com.evoq.ems.auth;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record FirstRunSetupRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull LocalDate hireDate,
        @NotBlank @Size(max = 100) String departmentName,
        @NotBlank @Size(max = 100) String jobTitle,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,99}",
                message = "Use 3–100 letters, numbers, dots, underscores or hyphens, starting with a letter or number") String username,
        @NotBlank @Size(min = 6, max = 72, message = "Use 6–72 characters") String password) {
}
