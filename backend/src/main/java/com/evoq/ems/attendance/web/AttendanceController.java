package com.evoq.ems.attendance.web;

import java.time.LocalDate;
import java.util.List;

import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.attendance.web.AttendanceDtos.RecordResponse;
import com.evoq.ems.attendance.web.AttendanceDtos.TeamOption;
import com.evoq.ems.attendance.web.AttendanceDtos.CorrectionRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.ExceptionRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.TodayResponse;
import com.evoq.ems.auth.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance-records")
public class AttendanceController {

    private final AttendanceService attendance;

    public AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping("/today")
    public TodayResponse today(@AuthenticationPrincipal AccountPrincipal principal) { return attendance.today(principal); }

    @PostMapping("/check-in")
    public RecordResponse checkIn(@AuthenticationPrincipal AccountPrincipal principal) { return attendance.checkIn(principal); }

    @PostMapping("/check-out")
    public RecordResponse checkOut(@AuthenticationPrincipal AccountPrincipal principal) { return attendance.checkOut(principal); }

    @GetMapping("/me")
    public List<RecordResponse> own(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendance.own(principal, from, to);
    }

    @GetMapping("/teams")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<TeamOption> teams(@AuthenticationPrincipal AccountPrincipal principal) {
        return attendance.managedTeams(principal);
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<RecordResponse> team(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendance.team(principal, teamId, from, to);
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public List<EmployeeOption> employees(@AuthenticationPrincipal AccountPrincipal principal) {
        return attendance.employeeOptions(principal);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public List<RecordResponse> all(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long employeeId) {
        return attendance.all(principal, from, to, employeeId);
    }

    @PostMapping("/exceptions")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<RecordResponse> createException(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ExceptionRequest request) {
        RecordResponse created = attendance.createException(principal, request);
        return ResponseEntity.created(java.net.URI.create("/api/attendance-records/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public RecordResponse correct(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody CorrectionRequest request) {
        return attendance.correct(principal, id, request);
    }
}
