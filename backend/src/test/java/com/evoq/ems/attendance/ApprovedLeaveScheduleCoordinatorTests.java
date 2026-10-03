package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.service.ApprovedLeaveScheduleCoordinator;
import org.junit.jupiter.api.Test;

class ApprovedLeaveScheduleCoordinatorTests {
    private final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    private final AttendanceRecordRepository attendance = mock(AttendanceRecordRepository.class);
    private final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T08:00:00Z"), ZoneId.of("UTC"));
    private final ApprovedLeaveScheduleCoordinator coordinator =
            new ApprovedLeaveScheduleCoordinator(entries, attendance, people, clock);
    private final LocalDate today = LocalDate.of(2026, 10, 1);

    @Test
    void approvalRemovesOnlyFuturePublishedShifts() {
        ScheduleEntry future = new ScheduleEntry(41L, 3L, today, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ScheduleEntry past = new ScheduleEntry(41L, 3L, today, LocalTime.of(7, 0), LocalTime.of(7, 30), null);
        when(entries.findEmployeeEntries(3L, today, today, Schedule.Status.PUBLISHED))
                .thenReturn(List.of(past, future));

        assertTrue(coordinator.removeFutureShifts(3L, today, today));
        verify(entries).deleteAll(List.of(future));
    }

    @Test
    void attendanceOnFutureDatePreventsRemoval() {
        ScheduleEntry future = new ScheduleEntry(41L, 3L, today, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(entries.findEmployeeEntries(3L, today, today, Schedule.Status.PUBLISHED))
                .thenReturn(List.of(future));
        when(attendance.existsByEmployeeIdAndAttendanceDate(3L, today)).thenReturn(true);

        assertFalse(coordinator.removeFutureShifts(3L, today, today));
        verify(entries, never()).deleteAll(any());
    }
}
