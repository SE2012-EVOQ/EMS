package com.evoq.ems.asset.controller;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.service.AssetService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.evoq.ems.auth.AccountPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // Register new asset
    @PostMapping
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public Asset registerAsset(@RequestBody Asset asset) {
        return assetService.registerAsset(asset);
    }

    // View all assets
    @GetMapping
    public List<Asset> getAllAssets(@AuthenticationPrincipal AccountPrincipal principal) {
        return assetService.getAllAssets(principal);
    }

    // View one asset
    @GetMapping("/{id}")
    public Asset getAssetById(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        return assetService.getAssetById(principal, id);
    }

    // Update asset
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public Asset updateAsset(
            @PathVariable Long id,
            @RequestBody Asset asset) {

        return assetService.updateAsset(id, asset);
    }

    // Update asset status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public Asset updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return assetService.updateStatus(id, status);
    }
}