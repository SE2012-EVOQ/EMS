package com.evoq.ems.employee.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.service.EmployeeService;
import com.evoq.ems.employee.web.EmployeeDtos.CreateEmployeeRequest;
import com.evoq.ems.employee.web.EmployeeDtos.EmployeeResponse;
import com.evoq.ems.employee.web.EmployeeDtos.StatusChangeRequest;
import com.evoq.ems.employee.web.EmployeeDtos.UpdateOfficialInfoRequest;
import com.evoq.ems.employee.web.EmployeeDtos.UpdatePersonalContactRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.ok(employeeService.getAllEmployees(principal, departmentId, teamId, status, search));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile(@AuthenticationPrincipal AccountPrincipal principal) {
        if (principal == null) {
            throw EmployeeModuleException.forbidden("User is not authenticated");
        }
        return ResponseEntity.ok(employeeService.getProfileByUsername(principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.ok(employeeService.getEmployeeById(principal, id));
    }

    @GetMapping("/{id}/direct-reports")
    @PreAuthorize("hasAnyRole('MANAGER_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<EmployeeResponse>> getDirectReports(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.ok(employeeService.getDirectReports(principal, id));
    }

    @GetMapping("/supervisor-candidates")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public List<EmployeeResponse> supervisorCandidates() { return employeeService.getSupervisorCandidates(); }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/official")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<EmployeeResponse> updateOfficialInfo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfficialInfoRequest request) {
        return ResponseEntity.ok(employeeService.updateOfficialInfo(id, request));
    }

    @PutMapping("/{id}/contact")
    public ResponseEntity<EmployeeResponse> updatePersonalContact(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonalContactRequest request,
            @AuthenticationPrincipal AccountPrincipal principal) {
        boolean isManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER_ADMIN"));
        if (!isManager && !id.equals(principal.getEmployeeId())) {
            throw EmployeeModuleException.forbidden("You can only update your own personal contact information");
        }
        return ResponseEntity.ok(employeeService.updatePersonalContact(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<EmployeeResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(employeeService.changeEmployeeStatus(id, request.status()));
    }
}
