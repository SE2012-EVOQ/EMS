package com.evoq.ems.employee.web;

import java.time.LocalDate;

import com.evoq.ems.employee.domain.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record CreateEmployeeRequest(
            @NotNull(message = "Department ID is required")
            Long departmentId,

            Long teamId,

            Long supervisorId,

            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must not exceed 100 characters")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must not exceed 100 characters")
            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 150, message = "Email must not exceed 150 characters")
            String email,

            @Size(max = 30, message = "Phone must not exceed 30 characters")
            String phone,

            @Size(max = 255, message = "Address must not exceed 255 characters")
            String address,

            @NotNull(message = "Hire date is required")
            LocalDate hireDate,

            @NotBlank(message = "Job title is required")
            @Size(max = 100, message = "Job title must not exceed 100 characters")
            String jobTitle,

            EmployeeStatus status,

            // Optional account setup
            boolean createAccount,
            String username,
            String password,
            String role
    ) {
    }

    public record UpdateOfficialInfoRequest(
            @NotNull(message = "Department ID is required")
            Long departmentId,

            Long teamId,

            Long supervisorId,

            @NotBlank(message = "Job title is required")
            @Size(max = 100, message = "Job title must not exceed 100 characters")
            String jobTitle,

            EmployeeStatus status,

            String role,
            @Size(min = 1, max = 100) String firstName,
            @Size(min = 1, max = 100) String lastName,
            @Email @Size(min = 1, max = 150) String email,
            LocalDate hireDate
    ) {
        public UpdateOfficialInfoRequest(Long departmentId, Long teamId, Long supervisorId,
                String jobTitle, EmployeeStatus status, String role) {
            this(departmentId, teamId, supervisorId, jobTitle, status, role, null, null, null, null);
        }
    }

    public record UpdatePersonalContactRequest(
            @Size(max = 30, message = "Phone must not exceed 30 characters")
            String phone,

            @Size(max = 255, message = "Address must not exceed 255 characters")
            String address
    ) {
    }

    public record EmployeeResponse(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String phone,
            String address,
            LocalDate hireDate,
            String jobTitle,
            EmployeeStatus status,
            DepartmentDto department,
            TeamDto team,
            SupervisorDto supervisor,
            AccountSummaryDto account
    ) {
    }

    public record DepartmentDto(
            Long id,
            String name,
            String description,
            Long employeeCount
    ) {
    }

    public record TeamDto(
            Long id,
            String name,
            String description
    ) {
    }

    public record SupervisorDto(
            Long id,
            String fullName,
            String email,
            String jobTitle
    ) {
    }

    public record AccountSummaryDto(
            Long id,
            String username,
            String role,
            boolean active
    ) {
    }

    public record DepartmentRequest(
            @NotBlank(message = "Department name is required")
            @Size(max = 100, message = "Department name must not exceed 100 characters")
            String name,

            @Size(max = 255, message = "Description must not exceed 255 characters")
            String description
    ) {
    }

    public record TeamRequest(
            @NotBlank(message = "Team name is required")
            @Size(max = 100, message = "Team name must not exceed 100 characters")
            String name,

            @Size(max = 255, message = "Description must not exceed 255 characters")
            String description
    ) {
    }

    public record StatusChangeRequest(
            @NotNull(message = "Status is required")
            EmployeeStatus status
    ) {
    }
}
