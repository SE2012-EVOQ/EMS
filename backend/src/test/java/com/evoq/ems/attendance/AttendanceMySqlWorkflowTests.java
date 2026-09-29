package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.service.ScheduleService;
import com.evoq.ems.attendance.web.ScheduleDtos.EntryRequest;
import com.evoq.ems.attendance.web.ScheduleDtos.WriteRequest;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Runs against the local MySQL schema and rolls all test business rows back. */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AttendanceMySqlWorkflowTests {

    @Autowired ScheduleService schedules;
    @Autowired ScheduleEntryRepository entries;
    @Autowired AttendanceRecordRepository records;
    @Autowired EmployeeTeamReader people;
    @Autowired ApprovedLeaveReader leaves;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publishedShiftCanDriveSelfCheckInAndCheckoutAgainstFrozenTables() {
        Long supervisorId = reservedEmployeeId("demo.supervisor");
        Long employeeId = reservedEmployeeId("demo.employee");
        LocalDate workDate = LocalDate.of(2099, 1, 15);
        while (!leaves.approvedConflicts(employeeId, workDate, workDate).isEmpty()) workDate = workDate.plusDays(1);
        Long teamId = people.employee(supervisorId).orElseThrow().teamId();

        AccountPrincipal supervisor = mock(AccountPrincipal.class);
        when(supervisor.getEmployeeId()).thenReturn(supervisorId);
        when(supervisor.getRole()).thenReturn("SUPERVISOR");
        var created = schedules.create(supervisor, new WriteRequest(teamId, workDate, workDate,
                List.of(new EntryRequest(null, employeeId, workDate, LocalTime.of(9, 0), LocalTime.of(17, 0), "MySQL workflow fixture"))));
        assertEquals(Schedule.Status.DRAFT, created.status());
        var published = schedules.publish(supervisor, created.id());
        assertEquals(Schedule.Status.PUBLISHED, published.status());
        assertEquals(1, entries.findPublishedEntriesForEmployeeDate(employeeId, workDate).size());

        AccountPrincipal employee = mock(AccountPrincipal.class);
        when(employee.getEmployeeId()).thenReturn(employeeId);
        when(employee.getRole()).thenReturn("EMPLOYEE");
        AttendanceService checkInService = attendanceAt("2099-01-15T09:07:00Z");
        var today = checkInService.today(employee);
        assertEquals(true, today.canCheckIn());
        var checkedIn = checkInService.checkIn(employee);
        assertEquals(AttendanceRecord.Status.PRESENT, checkedIn.status());
        assertEquals(LocalTime.of(9, 7), checkedIn.checkIn());
        assertEquals(new BigDecimal("0.00"), checkedIn.hours());
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> checkInService.checkIn(employee)).status());

        AttendanceService checkOutService = attendanceAt("2099-01-15T17:13:00Z");
        var checkedOut = checkOutService.checkOut(employee);
        assertEquals(LocalTime.of(17, 13), checkedOut.checkOut());
        assertEquals(new BigDecimal("8.10"), checkedOut.hours());
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> checkOutService.checkOut(employee)).status());
        assertEquals(1, records.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(
                employeeId, workDate, workDate).size());
    }

    private AttendanceService attendanceAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
        return new AttendanceService(records, entries, people, clock);
    }

    private Long reservedEmployeeId(String username) {
        return jdbc.queryForObject("""
                SELECT e.employee_id FROM employee e JOIN user_account u ON u.employee_id = e.employee_id
                WHERE u.username = ?
                """, Long.class, username);
    }
}
