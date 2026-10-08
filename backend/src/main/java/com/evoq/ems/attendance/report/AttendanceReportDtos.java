package com.evoq.ems.attendance.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.attendance.web.AttendanceDtos.TeamOption;

public final class AttendanceReportDtos {
    private AttendanceReportDtos() { }
    public enum Scope { MINE, TEAM, ORGANIZATION }
    public record Counts(long present, long late, long absent, long recordedLeave,
            long approvedLeaveDays, long overlapDays, long openCheckIns, BigDecimal workingHours) { }
    public record EmployeeSummary(Long employeeId, String employeeName, Counts counts) { }
    // Approved leave is an independent fact, not a synthetic AttendanceRecord or precedence rule.
    public record Day(Long employeeId, String employeeName, LocalDate date, String recordedStatus,
            LocalTime checkIn, LocalTime checkOut, BigDecimal workingHours, boolean approvedLeave) { }
    public record Report(LocalDate businessDate, String businessTimezone, Scope scope,
            LocalDate from, LocalDate to, Counts totals, List<EmployeeSummary> employees, List<Day> days) { }
    public record Options(List<TeamOption> teams, List<EmployeeOption> employees) { }
    public record Dashboard(Report attendance, long publishedShifts) { }
}
