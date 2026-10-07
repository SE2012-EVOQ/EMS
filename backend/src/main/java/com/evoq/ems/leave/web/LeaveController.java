package com.evoq.ems.leave.web;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.leave.web.LeaveDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/me")
    public ResponseEntity<LeaveOverviewResponse> getMyLeave(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.getMyLeave(principal.getEmployeeId())
        );
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeResponse>> getTypes() {
        return ResponseEntity.ok(leaveService.getTypes());
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public LeaveTypeResponse createType(@Valid @RequestBody TypeSetupRequest input) {
        return leaveService.saveType(null, input);
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public LeaveTypeResponse updateType(@PathVariable Long id, @Valid @RequestBody TypeSetupRequest input) {
        return leaveService.saveType(id, input);
    }

    @GetMapping("/setup/employees/{id}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public LeaveOverviewResponse employeeSetup(@PathVariable Long id) { return leaveService.getMyLeave(id); }

    @PutMapping("/setup/employees/{employeeId}/types/{typeId}")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public BalanceResponse setEntitlement(@PathVariable Long employeeId, @PathVariable Long typeId,
            @Valid @RequestBody EntitlementRequest input) {
        return leaveService.setEntitlement(employeeId, typeId, input);
    }

    @PostMapping("/requests")
    public ResponseEntity<RequestResponse> submit(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody SubmitLeaveRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        leaveService.submit(
                                principal.getEmployeeId(),
                                request
                        )
                );
    }

    @GetMapping("/supervisor/pending")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<RequestResponse>> getPending(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.getPendingForSupervisor(
                        principal.getEmployeeId()
                )
        );
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('MANAGER_ADMIN')")
    public ResponseEntity<List<RequestResponse>> getAllRequests() {
        return ResponseEntity.ok(
                leaveService.getAllRequests()
        );
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<RequestResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.approve(id, principal)
        );
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<RequestResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.reject(id, principal)
        );
    }
}
