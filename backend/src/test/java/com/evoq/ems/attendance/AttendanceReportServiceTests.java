package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.integration.*;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader.EmployeeLeave;
import com.evoq.ems.attendance.report.*;
import com.evoq.ems.attendance.report.AttendanceReportDtos.*;
import com.evoq.ems.attendance.repository.*;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendanceReportServiceTests {
    final AttendanceRecordRepository records = mock(AttendanceRecordRepository.class);
    final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    final ApprovedLeaveReader leaves = mock(ApprovedLeaveReader.class);
    final AttendanceService attendance = mock(AttendanceService.class);
    final AccountPrincipal principal = mock(AccountPrincipal.class);
    final LocalDate date = LocalDate.of(2026, 10, 7);
    final EmployeeInfo employee = new EmployeeInfo(3L, "Demo Employee", 7L, 2L, "ACTIVE");
    AttendanceReportService service;
    @BeforeEach void setup() {
        service = new AttendanceReportService(records, entries, people, leaves, attendance,
                Clock.fixed(Instant.parse("2026-10-06T20:00:00Z"), ZoneId.of("Asia/Colombo")));
        when(principal.getEmployeeId()).thenReturn(3L); when(principal.getRole()).thenReturn("EMPLOYEE");
        when(people.employee(3L)).thenReturn(Optional.of(employee));
    }
    @Test void independentLeaveAndRecordedStatusesDoNotInventPrecedenceOrRows() {
        when(records.findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(List.of(3L), date, date.plusDays(3)))
            .thenReturn(List.of(record(date, AttendanceRecord.Status.PRESENT, "7.92"), record(date.plusDays(1), AttendanceRecord.Status.LATE, "6.50"),
                record(date.plusDays(2), AttendanceRecord.Status.ABSENT, "0.00"), record(date.plusDays(3), AttendanceRecord.Status.LEAVE, "0.00")));
        when(leaves.approvedForEmployees(List.of(3L), date, date.plusDays(3))).thenReturn(List.of(
            new EmployeeLeave(3L, date.minusDays(2), date.plusDays(1)), new EmployeeLeave(3L, date, date.plusDays(1))));
        var report = service.report(principal, Scope.MINE, null, null, date, date.plusDays(3));
        assertEquals(new Counts(1, 1, 1, 1, 2, 2, 0, new BigDecimal("14.42")), report.totals());
        assertEquals(4, report.days().size()); assertTrue(report.days().getFirst().approvedLeave());
        assertEquals("PRESENT", report.days().getFirst().recordedStatus());
        verify(records, never()).save(any());
    }
    @Test void leaveWithoutScheduleOrRecordIsReadOnlyFact() {
        when(leaves.approvedForEmployees(List.of(3L), date, date)).thenReturn(List.of(new EmployeeLeave(3L, date, date)));
        var report = service.report(principal, Scope.MINE, null, null, date, date);
        assertNull(report.days().getFirst().recordedStatus()); assertEquals(1, report.totals().approvedLeaveDays());
        assertEquals(0, report.totals().recordedLeave()); verify(records, never()).save(any()); verifyNoInteractions(entries);
    }
    @Test void missingCalendarDayIsNotAnAbsence() {
        var report = service.report(principal, Scope.MINE, null, null, date, date);
        assertEquals(0, report.totals().absent()); assertTrue(report.days().isEmpty()); assertEquals(1, report.employees().size());
    }
    @Test void employeeCannotRequestTeamOrganizationOrAnotherIdentity() {
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.TEAM, 7L, null, date, date));
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.ORGANIZATION, null, null, date, date));
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.MINE, null, 9L, date, date));
        verifyNoInteractions(records, leaves);
    }
    @Test void supervisorOnlySeesCurrentActiveDirectReports() {
        when(principal.getRole()).thenReturn("SUPERVISOR"); when(principal.getEmployeeId()).thenReturn(2L);
        when(people.employee(2L)).thenReturn(Optional.of(new EmployeeInfo(2L, "Supervisor", 7L, null, "ACTIVE")));
        when(people.activeDirectReports(2L, 7L)).thenReturn(List.of(employee));
        assertEquals(3L, service.report(principal, Scope.TEAM, 7L, null, date, date).employees().getFirst().employeeId());
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.TEAM, 8L, null, date, date));
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.TEAM, 7L, 8L, date, date));
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.ORGANIZATION, null, null, date, date));
    }
    @Test void managerFiltersUseCurrentMembershipAndIncludeInactiveHistory() {
        when(principal.getRole()).thenReturn("MANAGER_ADMIN");
        var former = new EmployeeInfo(4L, "Former", 8L, null, "INACTIVE");
        when(people.allEmployees()).thenReturn(List.of(employee, former));
        when(people.team(8L)).thenReturn(Optional.of(new EmployeeTeamReader.TeamInfo(8L, "Other")));
        assertEquals(2, service.report(principal, Scope.ORGANIZATION, null, null, date, date).employees().size());
        assertEquals(4L, service.report(principal, Scope.ORGANIZATION, 8L, 4L, date, date).employees().getFirst().employeeId());
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.TEAM, 8L, 3L, date, date));
    }
    @Test void invalidRangesAndUnknownTeamsAreRejected() {
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.MINE, null, null, date, date.minusDays(1)));
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.MINE, null, null, date, date.plusDays(367)));
        when(principal.getRole()).thenReturn("MANAGER_ADMIN");
        assertThrows(AttendanceModuleException.class, () -> service.report(principal, Scope.TEAM, 999L, null, date, date));
    }
    @Test void businessDateAndDashboardAreServerControlled() {
        var result = service.dashboard(principal, Scope.MINE, null);
        assertEquals(date, result.attendance().businessDate()); assertEquals("Asia/Colombo", result.attendance().businessTimezone());
        verify(entries).findPublishedForEmployees(List.of(3L), date, date);
    }
    @Test void openCheckInsKeepTheirStoredZeroHours() {
        when(records.findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(List.of(3L), date, date))
            .thenReturn(List.of(new AttendanceRecord(3L, date, AttendanceRecord.Status.PRESENT, LocalTime.of(9, 0), null, new BigDecimal("0.00"), null)));
        assertEquals(1, service.report(principal, Scope.MINE, null, null, date, date).totals().openCheckIns());
    }
    @Test void existingCorrectedHoursAreReportedWithoutRewritingThem() {
        when(records.findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(List.of(3L), date, date))
            .thenReturn(List.of(record(date, AttendanceRecord.Status.LATE, "9.50")));
        assertEquals(new BigDecimal("9.50"), service.report(principal, Scope.MINE, null, null, date, date).totals().workingHours());
        verify(records, never()).save(any());
    }
    @Test void csvMatchesReportFactsAndEscapesSpreadsheetFormulas() {
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "  =HYPERLINK(\"x\"),Name", 7L, 2L, "ACTIVE")));
        when(leaves.approvedForEmployees(List.of(3L), date, date)).thenReturn(List.of(new EmployeeLeave(3L, date, date)));
        String csv = service.csv(service.report(principal, Scope.MINE, null, null, date, date));
        assertTrue(csv.contains("\"'  =HYPERLINK(\"\"x\"\"),Name\""));
        assertTrue(csv.contains("\"\",\"\",\"\",\"0.00\",\"true\",\"Asia/Colombo\"\r\n"));
    }
    AttendanceRecord record(LocalDate day, AttendanceRecord.Status status, String hours) {
        boolean worked = status == AttendanceRecord.Status.PRESENT || status == AttendanceRecord.Status.LATE;
        return new AttendanceRecord(3L, day, status, worked ? LocalTime.of(9, 5, 6) : null, worked ? LocalTime.of(17, 20, 37) : null, new BigDecimal(hours), null);
    }
}
