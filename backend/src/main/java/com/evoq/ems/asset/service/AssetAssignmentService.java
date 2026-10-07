package com.evoq.ems.asset.service;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.domain.AssetAssignment;
import com.evoq.ems.asset.repository.AssetAssignmentRepository;
import com.evoq.ems.asset.repository.AssetRepository;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.employee.domain.EmployeeStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AssetAssignmentService {
    private final AssetAssignmentRepository assignments;
    private final AssetRepository assets;
    private final EmployeeRepository employees;
    public AssetAssignmentService(AssetAssignmentRepository assignments, AssetRepository assets,
                                  EmployeeRepository employees) {
        this.assignments = assignments; this.assets = assets; this.employees = employees;
    }

    @Transactional
    public AssetAssignment assignAsset(Long assetId, Long employeeId) {
        // Employee first: also serializes with lifecycle changes. No mutation before validation.
        var employee = employees.findLockedById(employeeId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Employee not found"));
        if (employee.getStatus() != EmployeeStatus.ACTIVE)
            throw error(HttpStatus.BAD_REQUEST, "Select an active employee");
        Asset asset = assets.findLockedById(assetId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Asset not found"));
        if (!"AVAILABLE".equals(asset.getStatus()) || assignments.existsByAssetIdAndAssignmentStatus(assetId, "ASSIGNED"))
            throw error(HttpStatus.CONFLICT, "Asset is not available for assignment");
        AssetAssignment assignment = new AssetAssignment(assetId, employeeId, LocalDate.now(), null, "ASSIGNED");
        asset.setStatus("ASSIGNED");
        assets.save(asset);
        return assignments.save(assignment);
    }

    @Transactional
    public AssetAssignment returnAsset(Long assignmentId) {
        Long assetId = assignments.findAssetId(assignmentId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Assignment not found"));
        assets.findLockedById(assetId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Asset not found"));
        // Refresh after waiting on the asset lock so concurrent returns cannot reuse stale state.
        AssetAssignment assignment = assignments.findLockedById(assignmentId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!"ASSIGNED".equals(assignment.getAssignmentStatus()))
            throw error(HttpStatus.CONFLICT, "This assignment has already been returned");
        assignment.setReturnedDate(LocalDate.now());
        assignment.setAssignmentStatus("RETURNED");
        Asset asset = assets.findLockedById(assignment.getAssetId()).orElseThrow();
        asset.setStatus("AVAILABLE");
        assets.save(asset);
        return assignments.save(assignment);
    }
    public List<AssetAssignment> getAssignmentsByEmployee(Long id) { return assignments.findByEmployeeId(id); }
    public List<AssetAssignment> getAssignmentHistory(Long id) { return assignments.findByAssetId(id); }
    public List<AssetAssignment> getAllAssignments() { return assignments.findAll(); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
