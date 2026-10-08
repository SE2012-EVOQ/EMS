package com.evoq.ems.attendance.report;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.report.AttendanceReportDtos.*;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.attendance.web.AttendanceDtos.TeamOption;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceReportService {
    private final AttendanceRecordRepository records;
    private final ScheduleEntryRepository entries;
    private final EmployeeTeamReader people;
    private final ApprovedLeaveReader leaves;
    private final AttendanceService attendance;
    private final Clock clock;

    public AttendanceReportService(AttendanceRecordRepository records, ScheduleEntryRepository entries,
            EmployeeTeamReader people, ApprovedLeaveReader leaves, AttendanceService attendance, Clock attendanceClock) {
        this.records = records; this.entries = entries; this.people = people;
        this.leaves = leaves; this.attendance = attendance; this.clock = attendanceClock;
    }

    public Options options(AccountPrincipal principal, Scope scope, Long teamId) {
        var employees = permitted(principal, scope, teamId, null);
        List<TeamOption> teams = "MANAGER_ADMIN".equals(principal.getRole())
                ? people.allTeams().stream().map(t -> new TeamOption(t.id(), t.name())).toList()
                : "SUPERVISOR".equals(principal.getRole()) ? attendance.managedTeams(principal) : List.of();
        return new Options(teams, employees.stream().map(e -> new EmployeeOption(e.id(), e.name())).toList());
    }

    public Report report(AccountPrincipal principal, Scope scope, Long teamId, Long employeeId,
            LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366)
            throw error(HttpStatus.BAD_REQUEST, "Choose a valid date range of at most 366 days");
        var employees = permitted(principal, scope, teamId, employeeId);
        var ids = employees.stream().map(EmployeeInfo::id).toList();
        Map<Long, Map<LocalDate, AttendanceRecord>> byEmployee = new HashMap<>();
        if (!ids.isEmpty()) for (var record : records.findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(ids, from, to)) {
            byEmployee.computeIfAbsent(record.getEmployeeId(), k -> new TreeMap<>()).put(record.getAttendanceDate(), record);
        }
        Map<Long, Set<LocalDate>> leaveDays = new HashMap<>();
        for (var leave : leaves.approvedForEmployees(ids, from, to)) {
            LocalDate start = leave.startDate().isBefore(from) ? from : leave.startDate();
            LocalDate end = leave.endDate().isAfter(to) ? to : leave.endDate();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1))
                leaveDays.computeIfAbsent(leave.employeeId(), k -> new TreeSet<>()).add(date);
        }
        List<EmployeeSummary> summaries = new ArrayList<>();
        List<Day> days = new ArrayList<>();
        for (var employee : employees) {
            var recorded = byEmployee.getOrDefault(employee.id(), Map.of());
            var approved = leaveDays.getOrDefault(employee.id(), Set.of());
            Set<LocalDate> dates = new TreeSet<>(recorded.keySet()); dates.addAll(approved);
            List<Day> employeeDays = new ArrayList<>();
            for (var date : dates) {
                var r = recorded.get(date);
                employeeDays.add(new Day(employee.id(), employee.name(), date,
                        r == null ? null : r.getStatus().name(), r == null ? null : r.getCheckInTime(),
                        r == null ? null : r.getCheckOutTime(), r == null ? new BigDecimal("0.00") : r.getWorkingHours(), approved.contains(date)));
            }
            summaries.add(new EmployeeSummary(employee.id(), employee.name(), counts(employeeDays)));
            days.addAll(employeeDays);
        }
        return new Report(LocalDate.now(clock), clock.getZone().getId(), scope, from, to,
                counts(days), List.copyOf(summaries), List.copyOf(days));
    }

    public Dashboard dashboard(AccountPrincipal principal, Scope scope, Long teamId) {
        LocalDate date = LocalDate.now(clock);
        Report report = report(principal, scope, teamId, null, date, date);
        var ids = report.employees().stream().map(EmployeeSummary::employeeId).toList();
        long shifts = ids.isEmpty() ? 0 : entries.findPublishedForEmployees(ids, date, date).size();
        return new Dashboard(report, shifts);
    }

    private List<EmployeeInfo> permitted(AccountPrincipal principal, Scope scope, Long teamId, Long employeeId) {
        if (scope == null) throw error(HttpStatus.BAD_REQUEST, "Report scope is required");
        List<EmployeeInfo> employees;
        if (scope == Scope.MINE) {
            if (teamId != null || (employeeId != null && !employeeId.equals(principal.getEmployeeId())))
                throw error(HttpStatus.FORBIDDEN, "Own reports cannot select another employee or team");
            employees = List.of(people.employee(principal.getEmployeeId())
                    .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Employee access is unavailable")));
        } else if ("MANAGER_ADMIN".equals(principal.getRole())) {
            if (scope == Scope.TEAM && teamId == null) throw error(HttpStatus.BAD_REQUEST, "Choose a team");
            if (teamId != null && people.team(teamId).isEmpty()) throw error(HttpStatus.NOT_FOUND, "Team was not found");
            employees = people.allEmployees().stream().filter(e -> teamId == null || teamId.equals(e.teamId())).toList();
        } else if (scope == Scope.TEAM && "SUPERVISOR".equals(principal.getRole())) {
            // Preserve current active direct-report scope; historical membership is not stored (D-06).
            var supervisor = people.employee(principal.getEmployeeId())
                    .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Employee access is unavailable"));
            if (!supervisor.active() || teamId == null || !teamId.equals(supervisor.teamId()))
                throw error(HttpStatus.FORBIDDEN, "This team is not assigned to you");
            employees = people.activeDirectReports(supervisor.id(), teamId);
        } else throw error(HttpStatus.FORBIDDEN, "Access denied");
        if (employeeId == null) return employees;
        return List.of(employees.stream().filter(e -> employeeId.equals(e.id())).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Employee is outside the report scope")));
    }

    private Counts counts(List<Day> days) {
        long present = 0, late = 0, absent = 0, recordedLeave = 0, approved = 0, overlap = 0, open = 0;
        BigDecimal hours = new BigDecimal("0.00");
        for (var day : days) {
            if (day.recordedStatus() != null) switch (day.recordedStatus()) {
                case "PRESENT" -> present++;
                case "LATE" -> late++;
                case "ABSENT" -> absent++;
                case "LEAVE" -> recordedLeave++;
                default -> { }
            }
            if (day.approvedLeave()) { approved++; if (day.recordedStatus() != null) overlap++; }
            if (day.checkIn() != null && day.checkOut() == null) open++;
            hours = hours.add(day.workingHours());
        }
        return new Counts(present, late, absent, recordedLeave, approved, overlap, open, hours);
    }

    public String csv(Report report) {
        StringBuilder csv = new StringBuilder("Employee ID,Employee,Date,Recorded status,Check in,Check out,Recorded working hours,Approved leave,Business timezone\r\n");
        for (var day : report.days()) {
            var cells = List.of(day.employeeId().toString(), day.employeeName(), day.date().toString(),
                    day.recordedStatus() == null ? "" : day.recordedStatus(), day.checkIn() == null ? "" : day.checkIn().toString(),
                    day.checkOut() == null ? "" : day.checkOut().toString(), day.workingHours().toPlainString(),
                    Boolean.toString(day.approvedLeave()), report.businessTimezone());
            csv.append(cells.stream().map(this::csvCell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return csv.toString();
    }
    private String csvCell(String value) {
        // Neutralize spreadsheet formulas, including leading whitespace/control characters.
        String trimmed = value.stripLeading();
        if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    private AttendanceModuleException error(HttpStatus status, String message) { return new AttendanceModuleException(status, message); }
}
