package com.evoq.ems.employee.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evoq.ems.employee.service.OrganizationService;
import com.evoq.ems.employee.web.EmployeeDtos.DepartmentDto;
import com.evoq.ems.employee.web.EmployeeDtos.DepartmentRequest;
import com.evoq.ems.employee.web.EmployeeDtos.TeamDto;
import com.evoq.ems.employee.web.EmployeeDtos.TeamRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<List<DepartmentDto>> getDepartments() {
        return ResponseEntity.ok(organizationService.getAllDepartments());
    }

    @GetMapping("/departments/{id}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getDepartmentById(id));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentDto created = organizationService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamDto>> getTeams() {
        return ResponseEntity.ok(organizationService.getAllTeams());
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<TeamDto> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getTeamById(id));
    }

    @PostMapping("/teams")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<TeamDto> createTeam(@Valid @RequestBody TeamRequest request) {
        TeamDto created = organizationService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
