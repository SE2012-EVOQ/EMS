package com.evoq.ems.asset.repository;

import com.evoq.ems.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {

}