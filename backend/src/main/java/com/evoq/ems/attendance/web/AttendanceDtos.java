package com.evoq.ems.attendance.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AttendanceDtos {
    private AttendanceDtos() { }

    public record RecordResponse(Long id, Long employeeId, String employeeName, LocalDate date,
            AttendanceRecord.Status status, LocalTime checkIn, LocalTime checkOut,
            BigDecimal hours, String note) { }

    public record ExceptionRequest(@NotNull Long employeeId, @NotNull LocalDate date,
            @NotNull AttendanceRecord.Status status, LocalTime checkIn, LocalTime checkOut,
            @Size(max = 255) String note) { }

    public record CorrectionRequest(@NotNull AttendanceRecord.Status status,
            LocalTime checkIn, LocalTime checkOut, @Size(max = 255) String note) { }

    public record TodayResponse(LocalDate date, boolean scheduled, Long scheduleEntryId,
            LocalTime scheduledStart, LocalTime scheduledEnd, String checkInState,
            boolean canCheckIn, boolean canCheckOut, RecordResponse attendance) { }

    public record EmployeeOption(Long id, String name) { }
    public record TeamOption(Long id, String name) { }
}
