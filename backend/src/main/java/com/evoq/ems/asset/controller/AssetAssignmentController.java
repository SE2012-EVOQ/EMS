package com.evoq.ems.asset.controller;

import com.evoq.ems.asset.domain.AssetAssignment;
import com.evoq.ems.asset.service.AssetAssignmentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.service.EmployeeAccess;

import java.util.List;

@RestController
@RequestMapping("/api/asset-assignments")
public class AssetAssignmentController {

    private final AssetAssignmentService assignmentService;

    public AssetAssignmentController(
            AssetAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // Assign an asset to an employee
    @PostMapping("/assign")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public AssetAssignment assignAsset(
            @RequestParam Long assetId,
            @RequestParam Long employeeId) {

        return assignmentService.assignAsset(assetId, employeeId);
    }

    // Return an assigned asset
    @PutMapping("/{assignmentId}/return")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public AssetAssignment returnAsset(
            @PathVariable Long assignmentId) {

        return assignmentService.returnAsset(assignmentId);
    }

    // View all assignments
    @GetMapping
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public List<AssetAssignment> getAllAssignments() {
        return assignmentService.getAllAssignments();
    }

    // View assets assigned to a particular employee
    @GetMapping("/employee/{employeeId}")
    public List<AssetAssignment> getAssignmentsByEmployee(
            @PathVariable Long employeeId, @AuthenticationPrincipal AccountPrincipal principal) {
        EmployeeAccess.requireOwnOrManager(principal, employeeId);

        return assignmentService
                .getAssignmentsByEmployee(employeeId);
    }

    // View assignment history of a particular asset
    @GetMapping("/asset/{assetId}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public List<AssetAssignment> getAssignmentHistory(
            @PathVariable Long assetId) {

        return assignmentService
                .getAssignmentHistory(assetId);
    }
}