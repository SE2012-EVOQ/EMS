package com.evoq.ems.leave.web;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.common.ModuleReport;
import com.evoq.ems.leave.service.LeaveReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/leave-reports")
public class LeaveReportController {
    private final LeaveReportService reports;
    public LeaveReportController(LeaveReportService reports) { this.reports = reports; }
    @GetMapping
    public ModuleReport report(@AuthenticationPrincipal AccountPrincipal caller,
            @RequestParam(required = false) java.time.LocalDate from, @RequestParam(required = false) java.time.LocalDate to,
            @RequestParam(required = false) Long employeeId) {
        return reports.report(caller, from, to, employeeId);
    }
}
