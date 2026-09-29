package com.evoq.ems.employee.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evoq.ems.auth.Role;
import com.evoq.ems.auth.RoleRepository;
import com.evoq.ems.auth.UserAccount;
import com.evoq.ems.auth.UserAccountRepository;
import com.evoq.ems.employee.domain.Department;
import com.evoq.ems.employee.domain.Employee;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.domain.TeamProject;
import com.evoq.ems.employee.repository.DepartmentRepository;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.employee.repository.TeamProjectRepository;
import com.evoq.ems.employee.web.EmployeeDtos.AccountSummaryDto;
import com.evoq.ems.employee.web.EmployeeDtos.CreateEmployeeRequest;
import com.evoq.ems.employee.web.EmployeeDtos.DepartmentDto;
import com.evoq.ems.employee.web.EmployeeDtos.EmployeeResponse;
import com.evoq.ems.employee.web.EmployeeDtos.SupervisorDto;
import com.evoq.ems.employee.web.EmployeeDtos.TeamDto;
import com.evoq.ems.employee.web.EmployeeDtos.UpdateOfficialInfoRequest;
import com.evoq.ems.employee.web.EmployeeDtos.UpdatePersonalContactRequest;
import com.evoq.ems.employee.web.EmployeeModuleException;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamProjectRepository teamProjectRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           TeamProjectRepository teamProjectRepository,
                           UserAccountRepository userAccountRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.teamProjectRepository = teamProjectRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<EmployeeResponse> getAllEmployees(Long departmentId, Long teamId, EmployeeStatus status, String keyword) {
        List<Employee> list;
        if (keyword != null && !keyword.isBlank()) {
            list = employeeRepository.searchEmployees(keyword.trim());
        } else if (departmentId != null) {
            list = employeeRepository.findByDepartmentId(departmentId);
        } else if (teamId != null) {
            list = employeeRepository.findByTeamId(teamId);
        } else if (status != null) {
            list = employeeRepository.findByStatus(status);
        } else {
            list = employeeRepository.findAll();
        }

        // Apply any remaining in-memory filters when combined
        return list.stream()
                .filter(e -> departmentId == null || (e.getDepartment() != null && departmentId.equals(e.getDepartment().getId())))
                .filter(e -> teamId == null || (e.getTeam() != null && teamId.equals(e.getTeam().getId())))
                .filter(e -> status == null || status == e.getStatus())
                .map(this::toEmployeeResponse)
                .toList();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Employee not found with ID: " + id));
        return toEmployeeResponse(employee);
    }

    public EmployeeResponse getProfileByUsername(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> EmployeeModuleException.notFound("User account not found for username: " + username));
        return getEmployeeById(account.getEmployeeId());
    }

    public List<EmployeeResponse> getDirectReports(Long supervisorId) {
        return employeeRepository.findBySupervisorId(supervisorId).stream()
                .map(this::toEmployeeResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String email = request.email().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw EmployeeModuleException.conflict("Employee with email '" + email + "' already exists");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> EmployeeModuleException.notFound("Department not found with ID: " + request.departmentId()));

        TeamProject team = null;
        if (request.teamId() != null) {
            team = teamProjectRepository.findById(request.teamId())
                    .orElseThrow(() -> EmployeeModuleException.notFound("Team/Project not found with ID: " + request.teamId()));
        }

        Employee supervisor = null;
        if (request.supervisorId() != null) {
            supervisor = employeeRepository.findById(request.supervisorId())
                    .orElseThrow(() -> EmployeeModuleException.notFound("Supervisor not found with ID: " + request.supervisorId()));
        }

        Employee employee = new Employee(
                department,
                team,
                supervisor,
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone() != null ? request.phone().trim() : null,
                request.address() != null ? request.address().trim() : null,
                request.hireDate(),
                request.jobTitle().trim(),
                request.status() != null ? request.status() : EmployeeStatus.ACTIVE
        );

        Employee saved = employeeRepository.save(employee);

        // Provision user account if requested
        if (request.createAccount()) {
            provisionAccount(saved.getId(), request.username(), request.password(), request.role());
        }

        return toEmployeeResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateOfficialInfo(Long id, UpdateOfficialInfoRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Employee not found with ID: " + id));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> EmployeeModuleException.notFound("Department not found with ID: " + request.departmentId()));

        TeamProject team = null;
        if (request.teamId() != null) {
            team = teamProjectRepository.findById(request.teamId())
                    .orElseThrow(() -> EmployeeModuleException.notFound("Team/Project not found with ID: " + request.teamId()));
        }

        Employee supervisor = null;
        if (request.supervisorId() != null) {
            if (request.supervisorId().equals(id)) {
                throw EmployeeModuleException.badRequest("An employee cannot be their own supervisor");
            }
            supervisor = employeeRepository.findById(request.supervisorId())
                    .orElseThrow(() -> EmployeeModuleException.notFound("Supervisor not found with ID: " + request.supervisorId()));
        }

        employee.setDepartment(department);
        employee.setTeam(team);
        employee.setSupervisor(supervisor);
        employee.setJobTitle(request.jobTitle().trim());

        if (request.status() != null) {
            employee.setStatus(request.status());
            // Sync active state with linked user account
            userAccountRepository.findByEmployeeId(id).ifPresent(acc -> {
                acc.setActive(request.status() == EmployeeStatus.ACTIVE);
            });
        }

        // Update role if account exists and role is specified
        if (request.role() != null && !request.role().isBlank()) {
            Role role = roleRepository.findByName(request.role().trim().toUpperCase())
                    .orElseThrow(() -> EmployeeModuleException.notFound("Role not found: " + request.role()));
            userAccountRepository.findByEmployeeId(id).ifPresent(acc -> acc.setRole(role));
        }

        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated);
    }

    @Transactional
    public EmployeeResponse updatePersonalContact(Long id, UpdatePersonalContactRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Employee not found with ID: " + id));

        if (request.phone() != null) {
            employee.setPhone(request.phone().trim());
        }
        if (request.address() != null) {
            employee.setAddress(request.address().trim());
        }

        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated);
    }

    @Transactional
    public EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatus status) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Employee not found with ID: " + id));

        employee.setStatus(status);
        userAccountRepository.findByEmployeeId(id).ifPresent(acc -> {
            acc.setActive(status == EmployeeStatus.ACTIVE);
        });

        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated);
    }

    private void provisionAccount(Long employeeId, String username, String password, String roleName) {
        if (username == null || username.isBlank()) {
            throw EmployeeModuleException.badRequest("Username is required to create a user account");
        }
        String trimmedUsername = username.trim().toLowerCase();
        if (userAccountRepository.existsByUsername(trimmedUsername)) {
            throw EmployeeModuleException.conflict("Username '" + trimmedUsername + "' is already taken");
        }
        if (userAccountRepository.existsByEmployeeId(employeeId)) {
            throw EmployeeModuleException.conflict("An account already exists for employee ID: " + employeeId);
        }
        if (password == null || password.length() < 6) {
            throw EmployeeModuleException.badRequest("Password must be at least 6 characters long");
        }

        String targetRole = (roleName != null && !roleName.isBlank()) ? roleName.trim().toUpperCase() : "EMPLOYEE";
        Role role = roleRepository.findByName(targetRole)
                .orElseThrow(() -> EmployeeModuleException.notFound("Role not found: " + targetRole));

        UserAccount account = new UserAccount(
                employeeId,
                role,
                trimmedUsername,
                passwordEncoder.encode(password),
                true
        );
        userAccountRepository.save(account);
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        DepartmentDto deptDto = null;
        if (employee.getDepartment() != null) {
            deptDto = new DepartmentDto(
                    employee.getDepartment().getId(),
                    employee.getDepartment().getName(),
                    employee.getDepartment().getDescription(),
                    null
            );
        }

        TeamDto teamDto = null;
        if (employee.getTeam() != null) {
            teamDto = new TeamDto(
                    employee.getTeam().getId(),
                    employee.getTeam().getName(),
                    employee.getTeam().getDescription()
            );
        }

        SupervisorDto supDto = null;
        if (employee.getSupervisor() != null) {
            supDto = new SupervisorDto(
                    employee.getSupervisor().getId(),
                    employee.getSupervisor().getFullName(),
                    employee.getSupervisor().getEmail(),
                    employee.getSupervisor().getJobTitle()
            );
        }

        AccountSummaryDto accDto = userAccountRepository.findByEmployeeId(employee.getId())
                .map(acc -> new AccountSummaryDto(
                        acc.getId(),
                        acc.getUsername(),
                        acc.getRole().getName(),
                        acc.isActive()
                ))
                .orElse(null);

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getAddress(),
                employee.getHireDate(),
                employee.getJobTitle(),
                employee.getStatus(),
                deptDto,
                teamDto,
                supDto,
                accDto
        );
    }
}
