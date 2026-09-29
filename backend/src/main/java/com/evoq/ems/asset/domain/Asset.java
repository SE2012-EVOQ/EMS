package com.evoq.ems.asset.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetId;

    @Column(nullable = false)
    private String assetName;

    @Column(nullable = false)
    private String assetType;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String status;

    public Asset() {
    }

    public Asset(String assetName, String assetType,
                 String serialNumber, String status) {
        this.assetName = assetName;
        this.assetType = assetType;
        this.serialNumber = serialNumber;
        this.status = status;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}