package com.evoq.ems.employee.service;

import java.util.List;
import java.util.Optional;
import com.evoq.ems.auth.AccountPrincipal;

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

    public List<EmployeeResponse> getAllEmployees(AccountPrincipal caller, Long departmentId, Long teamId, EmployeeStatus status, String keyword) {
        List<Employee> list = new EmployeeAccess(employeeRepository).visibleEmployees(caller);

        // Apply any remaining in-memory filters when combined
        return list.stream()
                .filter(e -> keyword == null || keyword.isBlank() ||
                        (e.getFullName() + " " + e.getEmail() + " " + e.getJobTitle()).toLowerCase(java.util.Locale.ROOT)
                                .contains(keyword.trim().toLowerCase(java.util.Locale.ROOT)))
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

    public List<EmployeeResponse> getDirectReports(AccountPrincipal caller, Long supervisorId) {
        EmployeeAccess.requireCaller(caller);
        if (!EmployeeAccess.manager(caller) && !caller.getEmployeeId().equals(supervisorId))
            throw EmployeeModuleException.forbidden("You can only query your own direct reports");
        List<Long> visible = new EmployeeAccess(employeeRepository).visibleIds(caller);
        return employeeRepository.findBySupervisorId(supervisorId).stream()
                .filter(e -> visible.contains(e.getId()))
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
            supervisor = validSupervisor(request.supervisorId());
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
            provisionAccount(saved.getId(), request.username(), request.password(), request.role(), saved.getStatus() == EmployeeStatus.ACTIVE);
        }

        return toEmployeeResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateOfficialInfo(Long id, UpdateOfficialInfoRequest request) {
        Employee employee = employeeRepository.findLockedById(id)
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
            supervisor = validSupervisor(request.supervisorId());
        }

        if (request.firstName() != null) employee.setFirstName(requiredText(request.firstName()));
        if (request.lastName() != null) employee.setLastName(requiredText(request.lastName()));
        if (request.email() != null) {
            String email = requiredText(request.email()).toLowerCase(java.util.Locale.ROOT);
            if (employeeRepository.findByEmailIgnoreCase(email).filter(e -> !id.equals(e.getId())).isPresent())
                throw EmployeeModuleException.conflict("Employee email is already in use");
            employee.setEmail(email);
        }
        if (request.hireDate() != null) employee.setHireDate(request.hireDate());
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

        // PUT replaces both nullable contact fields; null/blank explicitly clears them.
        employee.setPhone(nullableText(request.phone()));
        employee.setAddress(nullableText(request.address()));

        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated);
    }

    @Transactional
    public EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatus status) {
        Employee employee = employeeRepository.findLockedById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Employee not found with ID: " + id));

        employee.setStatus(status);
        userAccountRepository.findByEmployeeId(id).ifPresent(acc -> {
            acc.setActive(status == EmployeeStatus.ACTIVE);
        });

        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated);
    }

    private String requiredText(String value) {
        if (value.isBlank()) throw EmployeeModuleException.badRequest("Official fields cannot be blank");
        return value.trim();
    }
    private String nullableText(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private Employee validSupervisor(Long id) {
        Employee candidate = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Supervisor not found"));
        boolean eligible = candidate.getStatus() == EmployeeStatus.ACTIVE &&
                userAccountRepository.findByEmployeeId(id).filter(a -> a.isActive() &&
                        List.of("SUPERVISOR", "MANAGER_ADMIN").contains(a.getRole().getName())).isPresent();
        if (!eligible) throw EmployeeModuleException.badRequest("Select an active Supervisor or Manager/Admin account");
        return candidate;
    }

    public List<EmployeeResponse> getSupervisorCandidates() {
        return employeeRepository.findByStatus(EmployeeStatus.ACTIVE).stream()
                .filter(e -> userAccountRepository.findByEmployeeId(e.getId()).filter(a -> a.isActive() &&
                        List.of("SUPERVISOR", "MANAGER_ADMIN").contains(a.getRole().getName())).isPresent())
                .map(this::toEmployeeResponse).toList();
    }

    public EmployeeResponse getEmployeeById(AccountPrincipal caller, Long id) {
        new EmployeeAccess(employeeRepository).requireVisible(caller, id);
        return getEmployeeById(id);
    }

    private void provisionAccount(Long employeeId, String username, String password, String roleName, boolean active) {
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
                active
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
