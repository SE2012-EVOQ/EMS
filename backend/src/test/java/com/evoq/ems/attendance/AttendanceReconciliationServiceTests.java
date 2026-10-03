package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.service.AttendanceReconciliationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttendanceReconciliationServiceTests {
    private final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    private final AttendanceRecordRepository records = mock(AttendanceRecordRepository.class);
    private final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    private final ApprovedLeaveReader leaves = mock(ApprovedLeaveReader.class);
    private final AttendanceReconciliationService service = new AttendanceReconciliationService(entries, records, people, leaves);
    private final LocalDate day = LocalDate.of(2026, 10, 1);

    @Test
    void missedShiftBecomesAbsentOnlyAfterScheduledEnd() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of(shift()));
        service.reconcile(3L, day, day.atTime(16, 59));
        verify(records, never()).save(any());

        service.reconcile(3L, day, day.atTime(17, 0));
        ArgumentCaptor<AttendanceRecord> saved = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(records).save(saved.capture());
        assertEquals(AttendanceRecord.Status.ABSENT, saved.getValue().getStatus());
    }

    @Test
    void openRecordClosesAtScheduledEndAndKeepsPreviousNote() {
        AttendanceRecord open = new AttendanceRecord(3L, day, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 7), null, BigDecimal.ZERO, "Original note");
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of(shift()));
        when(records.findByEmployeeIdAndAttendanceDate(3L, day)).thenReturn(Optional.of(open));

        service.reconcile(3L, day, day.plusDays(1).atStartOfDay());

        assertEquals(LocalTime.of(17, 0), open.getCheckOutTime());
        assertEquals(new BigDecimal("7.88"), open.getWorkingHours());
        assertEquals("Original note", open.getNotes());
    }

    @Test
    void existingExceptionOrRemovedShiftIsNotOverwritten() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of());
        service.reconcile(3L, day, day.plusDays(1).atStartOfDay());
        verify(records, never()).save(any());
    }

    @Test
    void approvedLeaveDoesNotBecomeAbsenceWhenStartedShiftRemainsPublished() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of(shift()));
        when(leaves.approvedConflicts(3L, day, day))
                .thenReturn(List.of(new ApprovedLeaveReader.LeaveConflict(day, day)));

        service.reconcile(3L, day, day.atTime(17, 1));

        verify(records, never()).save(any());
    }

    private ScheduleEntry shift() {
        return new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
    }
}
