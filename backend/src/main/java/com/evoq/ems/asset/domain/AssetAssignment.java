package com.evoq.ems.asset.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "asset_assignment")
public class AssetAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @Column(nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate assignedDate;

    private LocalDate returnedDate;

    @Column(name = "status", nullable = false)
    private String assignmentStatus;

    public AssetAssignment() {
    }

    public AssetAssignment(Long assetId, Long employeeId,
                           LocalDate assignedDate, LocalDate returnedDate,
                           String assignmentStatus) {
        this.assetId = assetId;
        this.employeeId = employeeId;
        this.assignedDate = assignedDate;
        this.returnedDate = returnedDate;
        this.assignmentStatus = assignmentStatus;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public void setReturnedDate(LocalDate returnedDate) {
        this.returnedDate = returnedDate;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
}