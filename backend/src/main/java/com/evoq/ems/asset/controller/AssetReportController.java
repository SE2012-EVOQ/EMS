package com.evoq.ems.asset.controller;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.common.ModuleReport;
import com.evoq.ems.asset.service.AssetReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/asset-reports")
public class AssetReportController {
    private final AssetReportService reports;
    public AssetReportController(AssetReportService reports) { this.reports = reports; }
    @GetMapping
    public ModuleReport report(@AuthenticationPrincipal AccountPrincipal caller,
            @RequestParam(required = false) Long employeeId) {
        return reports.report(caller, employeeId);
    }
}
