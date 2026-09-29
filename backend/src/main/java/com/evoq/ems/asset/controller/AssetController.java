package com.evoq.ems.asset.controller;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.service.AssetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // Register new asset
    @PostMapping
    public Asset registerAsset(@RequestBody Asset asset) {
        return assetService.registerAsset(asset);
    }

    // View all assets
    @GetMapping
    public List<Asset> getAllAssets() {
        return assetService.getAllAssets();
    }

    // View one asset
    @GetMapping("/{id}")
    public Asset getAssetById(@PathVariable Long id) {
        return assetService.getAssetById(id);
    }

    // Update asset
    @PutMapping("/{id}")
    public Asset updateAsset(
            @PathVariable Long id,
            @RequestBody Asset asset) {

        return assetService.updateAsset(id, asset);
    }

    // Update asset status
    @PatchMapping("/{id}/status")
    public Asset updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return assetService.updateStatus(id, status);
    }
}