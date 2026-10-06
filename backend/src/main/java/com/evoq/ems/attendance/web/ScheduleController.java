package com.evoq.ems.attendance.web;

import java.time.LocalDate;
import java.util.List;

import com.evoq.ems.attendance.service.ScheduleService;
import com.evoq.ems.attendance.web.ScheduleDtos.ScheduleResponse;
import com.evoq.ems.attendance.web.ScheduleDtos.TeamResponse;
import com.evoq.ems.attendance.web.ScheduleDtos.WriteRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.auth.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService schedules;

    public ScheduleController(ScheduleService schedules) { this.schedules = schedules; }

    @GetMapping("/me")
    public List<ScheduleResponse> own(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.ownPublished(principal, from, to);
    }

    @GetMapping("/teams")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public List<TeamResponse> teams(@AuthenticationPrincipal AccountPrincipal principal) {
        return schedules.managedTeams(principal);
    }

    @GetMapping("/teams/{teamId}/employees")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public List<EmployeeOption> employees(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long teamId) {
        return schedules.teamEmployees(principal, teamId);
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public List<ScheduleResponse> team(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeDrafts) {
        return schedules.teamSchedules(principal, teamId, from, to, includeDrafts);
    }

    @GetMapping("/{scheduleId}")
    public ScheduleResponse get(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long scheduleId) {
        return schedules.get(principal, scheduleId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public ResponseEntity<ScheduleResponse> create(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody WriteRequest request) {
        ScheduleResponse created = schedules.create(principal, request);
        return ResponseEntity.created(java.net.URI.create("/api/schedules/" + created.id())).body(created);
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public ScheduleResponse update(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long scheduleId, @Valid @RequestBody WriteRequest request) {
        return schedules.update(principal, scheduleId, request);
    }

    @PostMapping("/{scheduleId}/publish")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public ScheduleResponse publish(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long scheduleId) {
        return schedules.publish(principal, scheduleId);
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'MANAGER_ADMIN')")
    public ResponseEntity<Void> discardDraft(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long scheduleId) {
        schedules.discardDraft(principal, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
