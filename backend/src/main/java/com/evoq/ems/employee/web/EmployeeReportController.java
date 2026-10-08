package com.evoq.ems.employee.web;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.common.ModuleReport;
import com.evoq.ems.employee.service.EmployeeReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/employee-reports")
public class EmployeeReportController {
    private final EmployeeReportService reports;
    public EmployeeReportController(EmployeeReportService reports) { this.reports = reports; }
    @GetMapping
    public ModuleReport report(@AuthenticationPrincipal AccountPrincipal caller,
            @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) com.evoq.ems.employee.domain.EmployeeStatus status) {
        return reports.report(caller, departmentId, teamId, status);
    }
}
