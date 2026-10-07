package com.evoq.ems.employee.service;

import java.util.List;
import java.util.stream.Stream;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.domain.Employee;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.repository.EmployeeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Current membership scope, matching the established Attendance team boundary. */
@Service
@Transactional(readOnly = true)
public class EmployeeAccess {
    private final EmployeeRepository employees;
    public EmployeeAccess(EmployeeRepository employees) { this.employees = employees; }

    public List<Employee> visibleEmployees(AccountPrincipal caller) {
        requireCaller(caller);
        if (manager(caller)) return employees.findAll();
        Employee self = employees.findById(caller.getEmployeeId())
                .orElseThrow(() -> new AccessDeniedException("Employee profile unavailable"));
        if (!"SUPERVISOR".equals(caller.getRole()) || self.getTeam() == null) return List.of(self);
        return Stream.concat(Stream.of(self), employees.findBySupervisorId(caller.getEmployeeId()).stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE && e.getTeam() != null
                        && self.getTeam().getId().equals(e.getTeam().getId()))).distinct().toList();
    }

    public List<Long> visibleIds(AccountPrincipal caller) {
        return visibleEmployees(caller).stream().map(Employee::getId).toList();
    }

    public void requireVisible(AccountPrincipal caller, Long id) {
        requireCaller(caller);
        if (!manager(caller) && !visibleIds(caller).contains(id))
            throw new AccessDeniedException("Employee is outside your permitted scope");
    }

    public static boolean manager(AccountPrincipal caller) {
        return caller != null && "MANAGER_ADMIN".equals(caller.getRole());
    }
    public static void requireCaller(AccountPrincipal caller) {
        if (caller == null || caller.getEmployeeId() == null) throw new AccessDeniedException("Sign in required");
    }
    public static void requireOwnOrManager(AccountPrincipal caller, Long employeeId) {
        requireCaller(caller);
        if (!manager(caller) && !caller.getEmployeeId().equals(employeeId))
            throw new AccessDeniedException("You can only access your own asset assignments");
    }
}
