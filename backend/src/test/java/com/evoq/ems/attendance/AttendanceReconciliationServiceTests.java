package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.service.AttendanceReconciliationService;
import com.evoq.ems.attendance.service.AttendanceReconciliationJob;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.springframework.http.HttpStatus;
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
    void fixedClockJobClosesAtEndAndManualAfterReconciliationIsAConflict() {
        AttendanceRecord open = new AttendanceRecord(3L, day, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 5, 6), null, BigDecimal.ZERO, "Keep note");
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of(shift()));
        when(records.findByEmployeeIdAndAttendanceDate(3L, day)).thenReturn(Optional.of(open));
        when(entries.findPendingAttendanceBetweenDates(day.minusDays(366), day)).thenReturn(List.of(shift()));
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T11:30:00Z"), ZoneId.of("Asia/Colombo"));
        AttendanceReconciliationJob job = new AttendanceReconciliationJob(entries, service, clock);

        job.reconcileRecentShifts();
        job.reconcileRecentShifts();

        assertEquals(LocalTime.of(17, 0), open.getCheckOutTime());
        assertEquals(new BigDecimal("7.92"), open.getWorkingHours());
        assertEquals("Keep note", open.getNotes());
        verify(records).save(open);
        AccountPrincipal employee = mock(AccountPrincipal.class);
        when(employee.getEmployeeId()).thenReturn(3L);
        AttendanceService manual = new AttendanceService(records, entries, people, leaves, clock);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> manual.checkOut(employee)).status());
    }

    @Test
    void jobUsesConfiguredBusinessDateRatherThanUtcDateForItsPendingRange() {
        LocalDate businessDate = day.plusDays(1);
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T18:35:13Z"), ZoneId.of("Asia/Colombo"));

        new AttendanceReconciliationJob(entries, service, clock).reconcileRecentShifts();

        verify(entries).findPendingAttendanceBetweenDates(businessDate.minusDays(366), businessDate);
    }

    @Test
    void reconciliationAfterManualCheckoutPreservesActualTimestampAndCappedHours() {
        AttendanceRecord open = new AttendanceRecord(3L, day, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 5, 6), null, BigDecimal.ZERO, null);
        when(entries.findPublishedEntriesForEmployeeDate(3L, day)).thenReturn(List.of(shift()));
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, day)).thenReturn(List.of(open));
        when(records.findByEmployeeIdAndAttendanceDate(3L, day)).thenReturn(Optional.of(open));
        when(records.save(open)).thenReturn(open);
        AccountPrincipal employee = mock(AccountPrincipal.class);
        when(employee.getEmployeeId()).thenReturn(3L);
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T11:50:37Z"), ZoneId.of("Asia/Colombo"));

        new AttendanceService(records, entries, people, leaves, clock).checkOut(employee);
        service.reconcile(3L, day, java.time.LocalDateTime.now(clock));

        assertEquals(LocalTime.of(17, 20, 37), open.getCheckOutTime());
        assertEquals(new BigDecimal("7.92"), open.getWorkingHours());
        verify(records).save(open);
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
