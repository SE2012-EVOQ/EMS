package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceDtos.CorrectionRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.ExceptionRequest;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

class AttendanceServiceTests {

    private final AttendanceRecordRepository records = mock(AttendanceRecordRepository.class);
    private final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    private final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    private final ApprovedLeaveReader leaves = mock(ApprovedLeaveReader.class);
    private Clock clock;
    private AttendanceService service;
    private final LocalDate date = LocalDate.of(2026, 9, 29);
    private final AccountPrincipal employee = mock(AccountPrincipal.class);

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2026-09-29T09:00:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        when(employee.getEmployeeId()).thenReturn(3L);
        when(employee.getRole()).thenReturn("EMPLOYEE");
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "Demo Employee", 7L, 2L, "ACTIVE")));
    }

    @Test
    void checkInUsesServerIdentityDateAndTimeAtInclusiveStart() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        when(records.existsByEmployeeIdAndAttendanceDate(3L, date)).thenReturn(false);
        when(records.save(any(AttendanceRecord.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.checkIn(employee);

        assertEquals(date, result.date());
        assertEquals(LocalTime.of(9, 0), result.checkIn());
        assertEquals(new BigDecimal("0.00"), result.hours());
        assertEquals(AttendanceRecord.Status.PRESENT, result.status());
        verify(people).lockEmployee(3L);
    }

    @Test
    void approvedLeavePreventsCheckInEvenIfPublishedShiftStillExists() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        when(leaves.approvedConflicts(3L, date, date))
                .thenReturn(List.of(new ApprovedLeaveReader.LeaveConflict(date, date)));

        assertEquals("ON_LEAVE", service.today(employee).checkInState());
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(AttendanceModuleException.class, () -> service.checkIn(employee)).status());
        verify(records, never()).save(any());
    }

    @Test
    void approvedLeaveTodayWithoutShiftReportsLeaveWithoutCreatingAttendance() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of());
        when(leaves.approvedConflicts(3L, date, date))
                .thenReturn(List.of(new ApprovedLeaveReader.LeaveConflict(date.minusDays(1), date.plusDays(1))));

        var today = service.today(employee);

        assertEquals(date, today.date());
        assertEquals("ON_LEAVE", today.checkInState());
        assertEquals(false, today.scheduled());
        assertEquals(null, today.scheduleEntryId());
        assertEquals(null, today.scheduledStart());
        assertEquals(null, today.scheduledEnd());
        assertEquals(false, today.canCheckIn());
        assertEquals(false, today.canCheckOut());
        assertEquals(null, today.attendance());
        verify(leaves).approvedConflicts(3L, date, date);
        verify(records, never()).save(any());
    }

    @Test
    void noApprovedLeaveAndNoShiftReportsNoSchedule() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of());
        when(leaves.approvedConflicts(3L, date, date)).thenReturn(List.of());

        var today = service.today(employee);

        assertEquals("NO_SCHEDULE", today.checkInState());
        assertEquals(false, today.scheduled());
        assertEquals(false, today.canCheckIn());
        assertEquals(false, today.canCheckOut());
        assertEquals(null, today.attendance());
        verify(records, never()).save(any());
    }

    @Test
    void publishedShiftWithoutLeaveReportsAvailableCheckInAtStart() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        when(leaves.approvedConflicts(3L, date, date)).thenReturn(List.of());

        var today = service.today(employee);

        assertEquals("AVAILABLE", today.checkInState());
        assertEquals(true, today.scheduled());
        assertEquals(LocalTime.of(9, 0), today.scheduledStart());
        assertEquals(LocalTime.of(17, 0), today.scheduledEnd());
        assertEquals(true, today.canCheckIn());
        assertEquals(false, today.canCheckOut());
        assertEquals(null, today.attendance());
        verify(records, never()).save(any());
    }

    @Test
    void todayReportsAutomaticallyRecordedAbsence() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        when(records.findByEmployeeIdAndAttendanceDate(3L, date)).thenReturn(Optional.of(
                new AttendanceRecord(3L, date, AttendanceRecord.Status.ABSENT,
                        null, null, new BigDecimal("0.00"), "Automatically marked absent")));

        var today = service.today(employee);

        assertEquals("ABSENT", today.checkInState());
        assertEquals(false, today.canCheckIn());
    }

    @Test
    void checkInAllowsExactThirtyMinuteBoundaryAndRejectsOneSecondLater() {
        clock = Clock.fixed(Instant.parse("2026-09-29T09:30:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        when(records.existsByEmployeeIdAndAttendanceDate(3L, date)).thenReturn(false);
        when(records.save(any(AttendanceRecord.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals(LocalTime.of(9, 30), service.checkIn(employee).checkIn());

        clock = Clock.fixed(Instant.parse("2026-09-29T09:30:01Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> service.checkIn(employee)).status());
    }

    @Test
    void checkInRejectsBeforeShiftAndAfterWindowAndDuplicate() {
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        clock = Clock.fixed(Instant.parse("2026-09-29T08:59:59Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        assertThrows(AttendanceModuleException.class, () -> service.checkIn(employee));

        clock = Clock.fixed(Instant.parse("2026-09-29T09:01:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        when(records.existsByEmployeeIdAndAttendanceDate(3L, date)).thenReturn(true);
        assertThrows(AttendanceModuleException.class, () -> service.checkIn(employee));
    }

    @Test
    void checkoutCapsStandardHoursAtShiftEndAndPreservesActualServerTime() {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 7), null, new BigDecimal("0.00"), null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date)).thenReturn(List.of(open));
        when(records.save(open)).thenReturn(open);
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        clock = Clock.fixed(Instant.parse("2026-09-29T17:13:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);

        var response = service.checkOut(employee);

        assertEquals(LocalTime.of(17, 13), response.checkOut());
        assertEquals(new BigDecimal("7.88"), response.hours());
    }

    @ParameterizedTest
    @CsvSource({"16:40:13,7.59", "17:00:00,7.92", "17:20:37,7.92"})
    void checkoutBeforeAtAndAfterEndUsesSecondsAndHalfUpRounding(String checkout, String hours) {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 5, 6), null, BigDecimal.ZERO, null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date)).thenReturn(List.of(open));
        when(records.save(open)).thenReturn(open);
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        clock = Clock.fixed(Instant.parse("2026-09-29T" + checkout + "Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);

        var result = service.checkOut(employee);

        assertEquals(LocalTime.parse(checkout), result.checkOut());
        assertEquals(new BigDecimal(hours), result.hours());
    }

    @Test
    void duplicateManualCheckoutIsRejectedWithoutChangingTheSavedResult() {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 5, 6), null, BigDecimal.ZERO, null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date))
                .thenAnswer(call -> open.getCheckOutTime() == null ? List.of(open) : List.of());
        when(records.save(open)).thenReturn(open);
        when(entries.findPublishedEntriesForEmployeeDate(3L, date)).thenReturn(List.of(entry()));
        clock = Clock.fixed(Instant.parse("2026-09-29T17:20:37Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);

        service.checkOut(employee);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> service.checkOut(employee)).status());
        assertEquals(LocalTime.of(17, 20, 37), open.getCheckOutTime());
        assertEquals(new BigDecimal("7.92"), open.getWorkingHours());
        verify(records).save(open);
    }

    @Test
    void todayAndCheckInUseBusinessDateAndTimeAcrossUtcMidnightBoundary() {
        LocalDate businessDate = date.plusDays(1);
        ScheduleEntry shift = new ScheduleEntry(4L, 3L, businessDate,
                LocalTime.of(0, 0), LocalTime.of(8, 0), null);
        clock = Clock.fixed(Instant.parse("2026-09-29T18:35:13Z"), ZoneId.of("Asia/Colombo"));
        service = new AttendanceService(records, entries, people, leaves, clock);
        when(entries.findPublishedEntriesForEmployeeDate(3L, businessDate)).thenReturn(List.of(shift));
        when(records.save(any(AttendanceRecord.class))).thenAnswer(call -> call.getArgument(0));

        var today = service.today(employee);
        assertEquals(businessDate, today.date());
        assertEquals("AVAILABLE", today.checkInState());
        var checkedIn = service.checkIn(employee);
        assertEquals(businessDate, checkedIn.date());
        assertEquals(LocalTime.of(0, 5, 13), checkedIn.checkIn());
        assertEquals(AttendanceRecord.Status.PRESENT, checkedIn.status());
        verify(leaves, times(2)).approvedConflicts(3L, businessDate, businessDate);
    }

    @Test
    void checkoutWithoutPublishedShiftPreservesExceptionalRecordElapsedHours() {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 0), null, BigDecimal.ZERO, null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date)).thenReturn(List.of(open));
        when(records.save(open)).thenReturn(open);
        clock = Clock.fixed(Instant.parse("2026-09-29T17:20:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);

        assertEquals(new BigDecimal("8.33"), service.checkOut(employee).hours());
    }

    @Test
    void deactivatedEmployeeCanCloseAnExistingCheckIn() {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 0), null, BigDecimal.ZERO, null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date)).thenReturn(List.of(open));
        when(records.save(open)).thenReturn(open);
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "Demo Employee", 7L, 2L, "INACTIVE")));
        clock = Clock.fixed(Instant.parse("2026-09-29T17:00:00Z"), ZoneId.of("UTC"));
        service = new AttendanceService(records, entries, people, leaves, clock);

        assertEquals(LocalTime.of(17, 0), service.checkOut(employee).checkOut());
    }

    @Test
    void checkOutRejectsCheckoutAtOrBeforeCheckin() {
        AttendanceRecord open = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 0), null, BigDecimal.ZERO, null);
        when(records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(3L, date)).thenReturn(List.of(open));
        assertThrows(AttendanceModuleException.class, () -> service.checkOut(employee));
    }

    @Test
    void leaveCorrectionDoesNotQueryLeaveModuleAndCorrectionNoteIsOptional() {
        AccountPrincipal manager = mock(AccountPrincipal.class);
        when(manager.getRole()).thenReturn("MANAGER_ADMIN");
        AttendanceRecord existing = new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT,
                LocalTime.of(9, 0), null, BigDecimal.ZERO, null);
        when(records.findById(7L)).thenReturn(Optional.of(existing));
        when(records.save(existing)).thenReturn(existing);

        var response = service.correct(manager, 7L, new CorrectionRequest(AttendanceRecord.Status.LEAVE, null, null, null));
        assertEquals(AttendanceRecord.Status.LEAVE, response.status());
        assertEquals(date, response.date());
        verify(people, never()).activeEmployees();
    }

    @Test
    void managerCanCreateAnExceptionalRecordButCannotDuplicateEmployeeDate() {
        AccountPrincipal manager = mock(AccountPrincipal.class);
        when(manager.getRole()).thenReturn("MANAGER_ADMIN");
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "Demo Employee", 7L, 2L, "ACTIVE")));
        when(records.save(any(AttendanceRecord.class))).thenAnswer(call -> call.getArgument(0));
        var created = service.createException(manager,
                new ExceptionRequest(3L, date, AttendanceRecord.Status.ABSENT, null, null, null));
        assertEquals("Demo Employee", created.employeeName());
        when(records.existsByEmployeeIdAndAttendanceDate(3L, date)).thenReturn(true);
        assertThrows(AttendanceModuleException.class, () -> service.createException(manager,
                new ExceptionRequest(3L, date, AttendanceRecord.Status.ABSENT, null, null, null)));
    }

    @Test
    void managerCanRecordHistoricalExceptionForInactiveEmployee() {
        AccountPrincipal manager = mock(AccountPrincipal.class);
        when(manager.getRole()).thenReturn("MANAGER_ADMIN");
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "Former employee", 7L, 2L, "INACTIVE")));
        when(records.save(any(AttendanceRecord.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.createException(manager,
                new ExceptionRequest(3L, date, AttendanceRecord.Status.ABSENT, null, null, "Historical correction"));

        assertEquals("Former employee", created.employeeName());
    }

    @Test
    void employeeCannotCorrectAttendance() {
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AttendanceModuleException.class,
                () -> service.createException(employee,
                        new ExceptionRequest(3L, date, AttendanceRecord.Status.ABSENT, null, null, null))).status());
    }

    private ScheduleEntry entry() {
        return new ScheduleEntry(4L, 3L, date, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
    }
}
