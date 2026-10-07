package com.evoq.ems.attendance.report;

import java.time.LocalDate;
import com.evoq.ems.attendance.report.AttendanceReportDtos.*;
import com.evoq.ems.auth.AccountPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance-reports")
public class AttendanceReportController {
    private final AttendanceReportService reports;
    public AttendanceReportController(AttendanceReportService reports) { this.reports = reports; }

    @GetMapping("/options")
    public Options options(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "MINE") Scope scope, @RequestParam(required = false) Long teamId) {
        return reports.options(principal, scope, teamId);
    }
    @GetMapping
    public Report report(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "MINE") Scope scope, @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.report(principal, scope, teamId, employeeId, from, to);
    }
    @GetMapping("/csv")
    public ResponseEntity<String> csv(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "MINE") Scope scope, @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var report = reports.report(principal, scope, teamId, employeeId, from, to);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + from + "-" + to + ".csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(reports.csv(report));
    }
    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "MINE") Scope scope, @RequestParam(required = false) Long teamId) {
        return reports.dashboard(principal, scope, teamId);
    }
}
