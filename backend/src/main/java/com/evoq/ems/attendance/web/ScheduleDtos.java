package com.evoq.ems.attendance.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.evoq.ems.attendance.domain.Schedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ScheduleDtos {
    private ScheduleDtos() { }

    public record EntryRequest(Long id, @NotNull Long employeeId, @NotNull LocalDate workDate,
            @NotNull LocalTime startTime, @NotNull LocalTime endTime, @Size(max = 255) String notes) { }

    public record WriteRequest(@NotNull Long teamId, @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd, @NotNull List<@NotNull @Valid EntryRequest> entries,
            List<@NotNull Long> removedEntryIds) {
        public WriteRequest(Long teamId, LocalDate periodStart, LocalDate periodEnd, List<EntryRequest> entries) {
            this(teamId, periodStart, periodEnd, entries, List.of());
        }
    }

    public record EntryResponse(Long id, Long employeeId, String employeeName, LocalDate workDate,
            LocalTime startTime, LocalTime endTime, String notes) { }

    public record ScheduleResponse(Long id, Long teamId, String teamName, LocalDate periodStart,
            LocalDate periodEnd, Schedule.Status status, List<EntryResponse> entries) { }

    public record TeamResponse(Long id, String name) { }
}
