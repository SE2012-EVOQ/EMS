package com.evoq.ems.asset.repository;

import com.evoq.ems.asset.domain.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetAssignmentRepository
        extends JpaRepository<AssetAssignment, Long> {

    List<AssetAssignment> findByEmployeeId(Long employeeId);

    List<AssetAssignment> findByAssetId(Long assetId);
}