package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import com.evoq.ems.attendance.report.AttendanceReportService;
import com.evoq.ems.attendance.report.AttendanceReportDtos.Scope;
import com.evoq.ems.auth.AccountPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AttendanceReportMySqlTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AttendanceReportService reports;
    @Test void readOnlyReportJoinsApprovedLeaveWithoutWritingAttendance() {
        String name="AS07-"+UUID.randomUUID();
        jdbc.update("INSERT INTO department(name) VALUES (?)", name);
        Long department=jdbc.queryForObject("SELECT department_id FROM department WHERE name=?",Long.class,name);
        jdbc.update("INSERT INTO employee(department_id,first_name,last_name,email,hire_date,job_title,status) VALUES (?,'Report',?,?,'2026-01-01','Test','ACTIVE')", department,name,name+"@evoq.invalid");
        Long employee=jdbc.queryForObject("SELECT employee_id FROM employee WHERE email=?",Long.class,name+"@evoq.invalid");
        jdbc.update("INSERT INTO leave_type(name) VALUES (?)",name);
        Long type=jdbc.queryForObject("SELECT leave_type_id FROM leave_type WHERE name=?",Long.class,name);
        jdbc.update("INSERT INTO leave_request(employee_id,leave_type_id,start_date,end_date,status) VALUES (?,?,'2099-07-01','2099-07-03','APPROVED')",employee,type);
        jdbc.update("INSERT INTO attendance_record(employee_id,attendance_date,status,check_in_time,check_out_time,working_hours) VALUES (?,'2099-07-02','PRESENT','09:05:06','17:20:37',7.92)", employee);
        jdbc.update("INSERT INTO leave_request(employee_id,leave_type_id,start_date,end_date,status) VALUES (?,?,'2099-07-04','2099-07-04','PENDING')",employee,type);
        var principal=mock(AccountPrincipal.class); when(principal.getEmployeeId()).thenReturn(employee); when(principal.getRole()).thenReturn("EMPLOYEE");
        var report=reports.report(principal,Scope.MINE,null,null,LocalDate.of(2099,7,2),LocalDate.of(2099,7,4));
        assertEquals(2,report.days().size()); assertEquals(2,report.totals().approvedLeaveDays());
        assertEquals(1,report.totals().present()); assertEquals(1,report.totals().overlapDays());
        assertEquals(0,report.totals().recordedLeave()); assertEquals(new BigDecimal("7.92"),report.totals().workingHours());
        assertEquals("PRESENT",report.days().getFirst().recordedStatus()); assertNull(report.days().getLast().recordedStatus());
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM attendance_record WHERE employee_id=?",Long.class,employee));
        assertTrue(reports.csv(report).contains("2099-07-03"));
    }
}
