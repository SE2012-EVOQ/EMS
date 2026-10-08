package com.evoq.ems.asset.repository;

import com.evoq.ems.asset.domain.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetAssignmentRepository
        extends JpaRepository<AssetAssignment, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT a.assetId FROM AssetAssignment a WHERE a.assignmentId = :id")
    java.util.Optional<Long> findAssetId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM AssetAssignment a WHERE a.assignmentId = :id")
    java.util.Optional<AssetAssignment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByAssetIdAndAssignmentStatus(Long assetId, String assignmentStatus);
    List<AssetAssignment> findByEmployeeId(Long employeeId);

    List<AssetAssignment> findByAssetId(Long assetId);
}