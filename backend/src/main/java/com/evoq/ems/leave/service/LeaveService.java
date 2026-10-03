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

    public LeaveOverviewResponse getMyLeave(Long employeeId) {
        ensureLeaveSetup();

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

    public List<LeaveTypeResponse> getTypes() {
        ensureLeaveSetup();

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

    public List<RequestResponse> getPendingForSupervisor(Long supervisorId) {
        return leaveRequestRepository.findPendingForSupervisor(supervisorId)
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
        LeaveRequest request = leaveRequestRepository.findLockedById(requestId)
                .orElseThrow(() -> notFound("Leave request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw conflict("Only pending leave requests can be decided");
        }

        Employee requester = request.getEmployee();

        if (requester.getSupervisor() == null ||
                !requester.getSupervisor().getId().equals(principal.getEmployeeId())) {
            throw forbidden("Only the employee's assigned supervisor can decide this request");
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

    private void ensureLeaveSetup() {

        createTypeIfMissing(
                "Annual Leave",
                "Planned annual leave"
        );

        createTypeIfMissing(
                "Medical / Sick Leave",
                "Leave for illness or medical reasons"
        );

        createTypeIfMissing(
                "Casual Leave",
                "Short personal or casual leave"
        );

        List<LeaveType> types = leaveTypeRepository.findAll();

        for (Employee employee : employeeRepository.findByStatus(EmployeeStatus.ACTIVE)) {

            for (LeaveType type : types) {

                if (leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeId(
                                employee.getId(),
                                type.getId()
                        )
                        .isPresent()) {
                    continue;
                }

                BigDecimal openingDays;

                switch (type.getName()) {
                    case "Annual Leave" ->
                            openingDays = BigDecimal.valueOf(14);

                    case "Medical / Sick Leave" ->
                            openingDays = BigDecimal.valueOf(10);

                    default ->
                            openingDays = BigDecimal.valueOf(5);
                }

                leaveBalanceRepository.save(
                        new LeaveBalance(
                                employee,
                                type,
                                openingDays
                        )
                );
            }
        }
    }

    private void createTypeIfMissing(String name, String description) {

        if (leaveTypeRepository.findByNameIgnoreCase(name).isEmpty()) {
            leaveTypeRepository.save(
                    new LeaveType(name, description)
            );
        }
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
