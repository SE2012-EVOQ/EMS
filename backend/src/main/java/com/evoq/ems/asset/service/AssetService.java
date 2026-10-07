package com.evoq.ems.asset.service;

import com.evoq.ems.asset.domain.Asset;
import com.evoq.ems.asset.repository.AssetRepository;
import com.evoq.ems.asset.repository.AssetAssignmentRepository;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.service.EmployeeAccess;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AssetService {
    private final AssetRepository assets;
    private final AssetAssignmentRepository assignments;
    public AssetService(AssetRepository assets, AssetAssignmentRepository assignments) {
        this.assets = assets; this.assignments = assignments;
    }
    @Transactional
    public Asset registerAsset(Asset asset) {
        if (asset.getAssetId() != null) throw error(HttpStatus.BAD_REQUEST, "New assets cannot supply an ID");
        validateFields(asset);
        asset.setAssetName(asset.getAssetName().trim());
        asset.setAssetType(asset.getAssetType().trim());
        asset.setSerialNumber(asset.getSerialNumber().trim());
        String status = asset.getStatus() == null ? "AVAILABLE" : asset.getStatus().trim().toUpperCase(java.util.Locale.ROOT);
        validateManualStatus(status);
        asset.setStatus(status);
        return assets.save(asset);
    }
    public List<Asset> getAllAssets(AccountPrincipal caller) {
        EmployeeAccess.requireCaller(caller);
        if (EmployeeAccess.manager(caller)) return assets.findAll();
        var ids = assignments.findByEmployeeId(caller.getEmployeeId()).stream()
                .filter(a -> "ASSIGNED".equals(a.getAssignmentStatus())).map(a -> a.getAssetId()).distinct().toList();
        return assets.findAllById(ids);
    }
    public Asset getAssetById(AccountPrincipal caller, Long id) {
        EmployeeAccess.requireCaller(caller);
        if (!EmployeeAccess.manager(caller) && getAllAssets(caller).stream().noneMatch(a -> a.getAssetId().equals(id)))
            throw new org.springframework.security.access.AccessDeniedException("Asset is outside your assigned assets");
        return assets.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Asset not found"));
    }
    @Transactional
    public Asset updateAsset(Long id, Asset update) {
        validateFields(update);
        Asset existing = locked(id);
        changeStatus(existing, update.getStatus());
        existing.setAssetName(update.getAssetName().trim());
        existing.setAssetType(update.getAssetType().trim());
        existing.setSerialNumber(update.getSerialNumber().trim());
        return assets.save(existing);
    }
    @Transactional
    public Asset updateStatus(Long id, String status) {
        Asset asset = locked(id); changeStatus(asset, status); return assets.save(asset);
    }
    private Asset locked(Long id) {
        return assets.findLockedById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Asset not found"));
    }
    private void changeStatus(Asset asset, String status) {
        if (status != null) status = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (assignments.existsByAssetIdAndAssignmentStatus(asset.getAssetId(), "ASSIGNED")) {
            if (!"ASSIGNED".equals(status)) throw error(HttpStatus.CONFLICT, "Return the active assignment before changing status");
        } else validateManualStatus(status);
        asset.setStatus(status);
    }
    private void validateManualStatus(String status) {
        if (status == null || status.isBlank() || status.length() > 30 || "ASSIGNED".equalsIgnoreCase(status))
            throw error(HttpStatus.BAD_REQUEST, "Status is required (up to 30 characters); ASSIGNED is managed through assignments");
    }
    private void validateFields(Asset asset) {
        if (invalid(asset.getAssetName(), 150) || invalid(asset.getAssetType(), 100) || invalid(asset.getSerialNumber(), 150))
            throw error(HttpStatus.BAD_REQUEST, "Asset name, type and serial number are required within their length limits");
    }
    private boolean invalid(String value, int max) { return value == null || value.isBlank() || value.trim().length() > max; }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
