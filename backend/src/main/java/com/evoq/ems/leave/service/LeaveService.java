package com.evoq.ems.leave.service;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.attendance.service.ApprovedLeaveScheduleCoordinator;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.employee.domain.Employee;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.leave.domain.LeaveBalance;
import com.evoq.ems.leave.domain.LeaveRequest;
import com.evoq.ems.leave.domain.LeaveType;
import com.evoq.ems.leave.repository.LeaveBalanceRepository;
import com.evoq.ems.leave.repository.LeaveRequestRepository;
import com.evoq.ems.leave.repository.LeaveTypeRepository;
import com.evoq.ems.leave.web.LeaveDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class LeaveService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ApprovedLeaveScheduleCoordinator schedules;
    private final EmployeeTeamReader people;

    public LeaveService(
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            ApprovedLeaveScheduleCoordinator schedules,
            EmployeeTeamReader people
    ) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.schedules = schedules;
        this.people = people;
    }

    @Transactional(readOnly = true)
    public LeaveOverviewResponse getMyLeave(Long employeeId) {

        List<BalanceResponse> balances = leaveBalanceRepository.findForEmployee(employeeId)
                .stream()
                .map(this::toBalance)
                .toList();

        List<RequestResponse> requests = leaveRequestRepository.findForEmployee(employeeId)
                .stream()
                .map(this::toRequest)
                .toList();

        return new LeaveOverviewResponse(balances, requests);
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getTypes() {

        return leaveTypeRepository.findAll()
                .stream()
                .map(type -> new LeaveTypeResponse(
                        type.getId(),
                        type.getName(),
                        type.getDescription()
                ))
                .toList();
    }

    @Transactional
    public RequestResponse submit(Long employeeId, SubmitLeaveRequest request) {

        people.lockEmployee(employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> notFound("Employee not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw badRequest("Inactive employees cannot submit leave requests");
        }

        if (request.startDate().isAfter(request.endDate())) {
            throw badRequest("End date must be on or after start date");
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> notFound("Leave type not found"));

        long days = calculateDays(request.startDate(), request.endDate());

        long overlapping = leaveRequestRepository.countOverlappingRequests(
                employeeId,
                request.startDate(),
                request.endDate()
        );

        if (overlapping > 0) {
            throw conflict("This date range overlaps an existing pending or approved leave request");
        }

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeId(employeeId, leaveType.getId())
                .orElseThrow(() -> badRequest("Leave balance is not configured"));

        if (balance.getAvailableDays().compareTo(BigDecimal.valueOf(days)) < 0) {
            throw badRequest("Insufficient leave balance");
        }

        LeaveRequest saved = leaveRequestRepository.save(
                new LeaveRequest(
                        employee,
                        leaveType,
                        request.startDate(),
                        request.endDate(),
                        request.reason()
                )
        );

        return toRequest(saved);
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getPendingForApprover(AccountPrincipal principal) {
        requireApprover(principal);
        var approver = people.employee(principal.getEmployeeId()).orElseThrow(() -> forbidden("Approver unavailable"));
        if (!approver.active()) return List.of();
        return leaveRequestRepository.findPendingForApprover(principal.getEmployeeId(), "MANAGER_ADMIN".equals(principal.getRole()))
                .stream()
                .map(this::toRequest)
                .toList();
    }

    public List<RequestResponse> getAllRequests() {
        return leaveRequestRepository.findAllForManager()
                .stream()
                .map(this::toRequest)
                .toList();
    }

    @Transactional
    public RequestResponse approve(Long requestId, AccountPrincipal principal) {
        return decide(requestId, principal, true);
    }

    @Transactional
    public RequestResponse reject(Long requestId, AccountPrincipal principal) {
        return decide(requestId, principal, false);
    }

    private RequestResponse decide(
            Long requestId,
            AccountPrincipal principal,
            boolean approve
    ) {
        requireApprover(principal);
        LeaveRequest request = leaveRequestRepository.findLockedById(requestId)
                .orElseThrow(() -> notFound("Leave request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw conflict("Only pending leave requests can be decided");
        }

        Employee requester = request.getEmployee();

        boolean own = requester.getId().equals(principal.getEmployeeId());
        // The approved exception is Manager/Admin self-approval only.
        if (own && (!"MANAGER_ADMIN".equals(principal.getRole()) || !approve)) {
            throw forbidden("You cannot decide your own leave request");
        }

        // Serialize with official-info/status edits, then read current membership rather
        // than the request's potentially cached Employee association.
        java.util.stream.Stream.of(requester.getId(), principal.getEmployeeId()).distinct().sorted().forEach(people::lockEmployee);
        var current = people.employee(requester.getId()).orElseThrow(() -> notFound("Employee not found"));
        var supervisor = own ? current : people.employee(principal.getEmployeeId()).orElseThrow(() -> forbidden("Approver unavailable"));
        if (!current.active() || !supervisor.active() || (!own && (current.teamId() == null
                || !current.teamId().equals(supervisor.teamId())
                || !principal.getEmployeeId().equals(current.supervisorId())))) {
            throw forbidden("Decisions require an active assigned direct report in your current team, or Manager/Admin approval of own leave");
        }

        if (approve) {
            if (!schedules.removeFutureShifts(requester.getId(), request.getStartDate(), request.getEndDate())) {
                throw conflict("An attendance record exists for a future shift in this leave period");
            }
            long days = calculateDays(
                    request.getStartDate(),
                    request.getEndDate()
            );

            LeaveBalance balance = leaveBalanceRepository
                    .findLockedForEmployeeType(
                            requester.getId(),
                            request.getLeaveType().getId()
                    )
                    .orElseThrow(() -> badRequest("Leave balance is not configured"));

            if (balance.getAvailableDays().compareTo(BigDecimal.valueOf(days)) < 0) {
                throw badRequest("Insufficient leave balance");
            }

            balance.approveDays(BigDecimal.valueOf(days));
            request.approve();
        } else {
            request.reject();
        }

        return toRequest(leaveRequestRepository.save(request));
    }

    private void requireApprover(AccountPrincipal principal) {
        if (principal == null || principal.getEmployeeId() == null
                || !("SUPERVISOR".equals(principal.getRole()) || "MANAGER_ADMIN".equals(principal.getRole()))) {
            throw forbidden("Only Supervisor or Manager/Admin can decide leave requests");
        }
    }

    @Transactional
    public LeaveTypeResponse saveType(Long id, TypeSetupRequest input) {
        String name = input.name().trim();
        if (leaveTypeRepository.findByNameIgnoreCase(name).filter(t -> !t.getId().equals(id)).isPresent())
            throw conflict("Leave type name already exists");
        LeaveType type = id == null ? new LeaveType(name, input.description()) : leaveTypeRepository.findById(id)
                .orElseThrow(() -> notFound("Leave type not found"));
        type.update(name, input.description());
        LeaveType saved = leaveTypeRepository.save(type);
        return new LeaveTypeResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public BalanceResponse setEntitlement(Long employeeId, Long typeId, EntitlementRequest input) {
        Employee employee = employeeRepository.findLockedById(employeeId)
                .orElseThrow(() -> notFound("Employee not found"));
        LeaveType type = leaveTypeRepository.findById(typeId).orElseThrow(() -> notFound("Leave type not found"));
        LeaveBalance balance = leaveBalanceRepository.findLockedForEmployeeType(employeeId, typeId)
                .orElseGet(() -> new LeaveBalance(employee, type, BigDecimal.ZERO));
        if (input.entitlementDays().compareTo(balance.getUsedDays()) < 0)
            throw badRequest("Entitlement cannot be less than days already used");
        balance.setEntitlement(input.entitlementDays());
        return toBalance(leaveBalanceRepository.save(balance));
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> reportRequests(AccountPrincipal caller) {
        var ids = new com.evoq.ems.employee.service.EmployeeAccess(employeeRepository).visibleIds(caller);
        return leaveRequestRepository.findForEmployees(ids).stream().map(this::toRequest).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeBalanceResponse> reportBalances(AccountPrincipal caller) {
        var ids = new com.evoq.ems.employee.service.EmployeeAccess(employeeRepository).visibleIds(caller);
        return leaveBalanceRepository.findForEmployees(ids).stream().map(balance -> new EmployeeBalanceResponse(
                balance.getEmployee().getId(), balance.getEmployee().getFullName(), toBalance(balance))).toList();
    }

    private BalanceResponse toBalance(LeaveBalance balance) {

        return new BalanceResponse(
                balance.getId(),
                balance.getLeaveType().getId(),
                balance.getLeaveType().getName(),
                balance.getAvailableDays(),
                balance.getUsedDays()
        );
    }

    private RequestResponse toRequest(LeaveRequest request) {

        return new RequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getEmployee().getFullName(),
                request.getLeaveType().getId(),
                request.getLeaveType().getName(),
                request.getStartDate(),
                request.getEndDate(),
                calculateDays(
                        request.getStartDate(),
                        request.getEndDate()
                ),
                request.getReason(),
                request.getStatus(),
                request.getSubmittedDate()
        );
    }

    private long calculateDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                message
        );
    }
}
