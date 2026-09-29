package com.evoq.ems.leave.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LeaveDtos {

    private LeaveDtos() {
    }

    public record LeaveTypeResponse(
            Long id,
            String name,
            String description
    ) {
    }

    public record BalanceResponse(
            Long id,
            Long leaveTypeId,
            String leaveType,
            BigDecimal availableDays,
            BigDecimal usedDays
    ) {
    }

    public record SubmitLeaveRequest(
            @NotNull(message = "Leave type is required")
            Long leaveTypeId,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate,

            @Size(max = 500, message = "Reason must not exceed 500 characters")
            String reason
    ) {
    }

    public record RequestResponse(
            Long id,
            Long employeeId,
            String employeeName,
            Long leaveTypeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            long days,
            String reason,
            String status,
            LocalDateTime submittedDate
    ) {
    }

    public record LeaveOverviewResponse(
            List<BalanceResponse> balances,
            List<RequestResponse> requests
    ) {
    }
}
