package com.evoq.ems.asset.service;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    // Register a new asset
    public Asset registerAsset(Asset asset) {
        if (asset.getStatus() == null || asset.getStatus().isBlank()) {
            asset.setStatus("AVAILABLE");
        }

        return assetRepository.save(asset);
    }

    // Get all assets
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    // Get one asset by ID
    public Asset getAssetById(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new RuntimeException("Asset not found with ID: " + assetId));
    }

    // Update an existing asset
    public Asset updateAsset(Long assetId, Asset updatedAsset) {

        Asset existingAsset = getAssetById(assetId);

        existingAsset.setAssetName(updatedAsset.getAssetName());
        existingAsset.setAssetType(updatedAsset.getAssetType());
        existingAsset.setSerialNumber(updatedAsset.getSerialNumber());
        existingAsset.setStatus(updatedAsset.getStatus());

        return assetRepository.save(existingAsset);
    }

    // Update only the asset status
    public Asset updateStatus(Long assetId, String status) {

        Asset asset = getAssetById(assetId);

        asset.setStatus(status);

        return assetRepository.save(asset);
    }
}