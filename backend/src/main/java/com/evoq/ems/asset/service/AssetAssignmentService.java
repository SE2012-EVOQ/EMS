package com.evoq.ems.asset.service;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.domain.AssetAssignment;
import com.evoq.ems.asset.repository.AssetAssignmentRepository;
import com.evoq.ems.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssetAssignmentService {

    private final AssetAssignmentRepository assignmentRepository;
    private final AssetRepository assetRepository;

    public AssetAssignmentService(
            AssetAssignmentRepository assignmentRepository,
            AssetRepository assetRepository) {

        this.assignmentRepository = assignmentRepository;
        this.assetRepository = assetRepository;
    }

    // Assign an asset to an employee
    public AssetAssignment assignAsset(Long assetId, Long employeeId) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new RuntimeException("Asset not found with ID: " + assetId));

        // Asset must be available before assignment
        if (!"AVAILABLE".equalsIgnoreCase(asset.getStatus())) {
            throw new IllegalArgumentException("Asset is not available for assignment");
        }

        AssetAssignment assignment = new AssetAssignment();

        assignment.setAssetId(assetId);
        assignment.setEmployeeId(employeeId);
        assignment.setAssignedDate(LocalDate.now());
        assignment.setReturnedDate(null);
        assignment.setAssignmentStatus("ASSIGNED");

        // Change asset status
        asset.setStatus("ASSIGNED");
        assetRepository.save(asset);

        return assignmentRepository.save(assignment);
    }

    // Return an assigned asset
    public AssetAssignment returnAsset(Long assignmentId) {

        AssetAssignment assignment =
                assignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Assignment not found with ID: "
                                                + assignmentId));

        if ("RETURNED".equalsIgnoreCase(
                assignment.getAssignmentStatus())) {

            throw new IllegalArgumentException(
                    "This asset has already been returned");
        }

        assignment.setReturnedDate(LocalDate.now());
        assignment.setAssignmentStatus("RETURNED");

        Asset asset = assetRepository
                .findById(assignment.getAssetId())
                .orElseThrow(() ->
                        new RuntimeException("Asset not found"));

        // Asset becomes available again
        asset.setStatus("AVAILABLE");
        assetRepository.save(asset);

        return assignmentRepository.save(assignment);
    }

    // View assignments for one employee
    public List<AssetAssignment> getAssignmentsByEmployee(
            Long employeeId) {

        return assignmentRepository.findByEmployeeId(employeeId);
    }

    // View assignment history of one asset
    public List<AssetAssignment> getAssignmentHistory(Long assetId) {

        return assignmentRepository.findByAssetId(assetId);
    }

    // View all assignment records
    public List<AssetAssignment> getAllAssignments() {

        return assignmentRepository.findAll();
    }
}