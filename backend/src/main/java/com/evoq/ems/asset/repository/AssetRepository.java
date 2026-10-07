package com.evoq.ems.asset.repository;

import com.evoq.ems.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Asset a WHERE a.assetId = :id")
    java.util.Optional<Asset> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);


}