package com.evoq.ems.attendance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.web.AttendanceDtos.CorrectionRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.attendance.web.AttendanceDtos.ExceptionRequest;
import com.evoq.ems.attendance.web.AttendanceDtos.RecordResponse;
import com.evoq.ems.attendance.web.AttendanceDtos.TeamOption;
import com.evoq.ems.attendance.web.AttendanceDtos.TodayResponse;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.auth.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository records;
    private final ScheduleEntryRepository entries;
    private final EmployeeTeamReader people;
    private final ApprovedLeaveReader leaves;
    private final Clock clock;

    public AttendanceService(AttendanceRecordRepository records, ScheduleEntryRepository entries,
            EmployeeTeamReader people, ApprovedLeaveReader leaves, Clock attendanceClock) {
        this.records = records;
        this.entries = entries;
        this.people = people;
        this.leaves = leaves;
        this.clock = attendanceClock;
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> own(AccountPrincipal principal, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return responses(records.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(
                principal.getEmployeeId(), from, to));
    }

    @Transactional(readOnly = true)
    public TodayResponse today(AccountPrincipal principal) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        LocalDate date = now.toLocalDate();
        List<ScheduleEntry> scheduled = entries.findPublishedEntriesForEmployeeDate(principal.getEmployeeId(), date);
        if (scheduled.size() > 1) throw conflict("More than one published shift exists for today; contact an administrator");
        AttendanceRecord record = records.findByEmployeeIdAndAttendanceDate(principal.getEmployeeId(), date).orElse(null);
        ScheduleEntry entry = scheduled.isEmpty() ? null : scheduled.getFirst();
        String state;
        boolean canIn = false;
        boolean canOut = record != null && record.getCheckInTime() != null && record.getCheckOutTime() == null;
        if (record != null && record.getCheckOutTime() != null) state = "COMPLETED";
        else if (canOut) state = "CHECKED_IN";
        else if (record != null && record.getStatus() == AttendanceRecord.Status.ABSENT) state = "ABSENT";
        else if (record != null && record.getStatus() == AttendanceRecord.Status.LEAVE) state = "ON_LEAVE";
        else if (record != null) state = "ALREADY_RECORDED";
        else if (!leaves.approvedConflicts(principal.getEmployeeId(), date, date).isEmpty()) state = "ON_LEAVE";
        else if (entry == null) state = "NO_SCHEDULE";
        else if (now.isBefore(date.atTime(entry.getStartTime()))) state = "NOT_OPEN";
        else if (now.isAfter(date.atTime(entry.getStartTime()).plusMinutes(30))
                || !now.isBefore(date.atTime(entry.getEndTime()))) state = "WINDOW_CLOSED";
        else { state = "AVAILABLE"; canIn = true; }
        return new TodayResponse(date, entry != null, entry == null ? null : entry.getId(),
                entry == null ? null : entry.getStartTime(), entry == null ? null : entry.getEndTime(),
                state, canIn, canOut, record == null ? null : responses(List.of(record)).getFirst());
    }

    @Transactional
    public RecordResponse checkIn(AccountPrincipal principal) {
        Long employeeId = principal.getEmployeeId();
        people.lockEmployee(employeeId);
        activeEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        LocalDate date = now.toLocalDate();
        List<ScheduleEntry> found = entries.findPublishedEntriesForEmployeeDate(employeeId, date);
        if (found.size() != 1) throw conflict(found.isEmpty()
                ? "No published schedule entry exists for today" : "More than one published shift exists for today");
        ScheduleEntry entry = found.getFirst();
        if (!leaves.approvedConflicts(employeeId, date, date).isEmpty()) {
            throw conflict("Check-in is unavailable during approved leave");
        }
        LocalTime time = now.toLocalTime();
        LocalDateTime scheduledStart = date.atTime(entry.getStartTime());
        if (now.isBefore(scheduledStart) || now.isAfter(scheduledStart.plusMinutes(30))
                || !now.isBefore(date.atTime(entry.getEndTime()))) {
            throw conflict("Check-in is available from the scheduled start until the earlier of the shift end or 30 minutes later");
        }
        if (records.existsByEmployeeIdAndAttendanceDate(employeeId, date)) {
            throw conflict("Attendance has already been recorded for today");
        }
        AttendanceRecord record = records.save(new AttendanceRecord(employeeId, date, AttendanceRecord.Status.PRESENT,
                time, null, BigDecimal.ZERO.setScale(2), null));
        return responses(List.of(record)).getFirst();
    }

    @Transactional
    public RecordResponse checkOut(AccountPrincipal principal) {
        Long employeeId = principal.getEmployeeId();
        people.lockEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        List<AttendanceRecord> open = records.findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(employeeId, now.toLocalDate())
                .stream().filter(record -> record.getCheckInTime() != null).toList();
        if (open.size() != 1) throw conflict(open.isEmpty()
                ? "No open check-in exists for today" : "Multiple open check-ins exist; contact an administrator");
        AttendanceRecord record = open.getFirst();
        LocalTime checkout = now.toLocalTime();
        if (!checkout.isAfter(record.getCheckInTime())) throw conflict("Check-out must be after check-in on the same day");
        List<ScheduleEntry> scheduled = entries.findPublishedEntriesForEmployeeDate(employeeId, record.getAttendanceDate());
        if (scheduled.size() > 1) throw conflict("More than one published shift exists for today; contact an administrator");
        LocalTime hoursEnd = scheduled.isEmpty() || checkout.isBefore(scheduled.getFirst().getEndTime())
                ? checkout : scheduled.getFirst().getEndTime();
        if (!hoursEnd.isAfter(record.getCheckInTime())) {
            throw conflict("Scheduled shift end must be after check-in; contact an administrator for correction");
        }
        record.checkOut(checkout, calculateHours(record.getCheckInTime(), hoursEnd));
        return responses(List.of(records.save(record))).getFirst();
    }

    @Transactional(readOnly = true)
    public List<TeamOption> managedTeams(AccountPrincipal principal) {
        requireRole(principal, "SUPERVISOR");
        EmployeeInfo supervisor = activeEmployee(principal.getEmployeeId());
        if (supervisor.teamId() == null || people.activeDirectReportIds(supervisor.id(), supervisor.teamId()).isEmpty()) return List.of();
        return people.team(supervisor.teamId()).map(team -> List.of(new TeamOption(team.id(), team.name()))).orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> team(AccountPrincipal principal, Long teamId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        requireRole(principal, "SUPERVISOR");
        EmployeeInfo supervisor = activeEmployee(principal.getEmployeeId());
        if (supervisor.teamId() == null || !supervisor.teamId().equals(teamId)) throw forbidden("This team is not assigned to you");
        List<Long> ids = people.activeDirectReportIds(supervisor.id(), teamId);
        if (ids.isEmpty()) return List.of();
        return responses(records.findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(ids, from, to));
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> all(AccountPrincipal principal, LocalDate from, LocalDate to, Long employeeId) {
        validateRange(from, to);
        requireRole(principal, "MANAGER_ADMIN");
        List<AttendanceRecord> found = employeeId == null
                ? records.findByAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(from, to)
                : records.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(employeeId, from, to);
        return responses(found);
    }

    @Transactional(readOnly = true)
    public List<EmployeeOption> employeeOptions(AccountPrincipal principal) {
        requireRole(principal, "MANAGER_ADMIN");
        return people.allEmployees().stream().map(employee -> new EmployeeOption(employee.id(), employee.name())).toList();
    }

    @Transactional
    public RecordResponse createException(AccountPrincipal principal, ExceptionRequest request) {
        requireRole(principal, "MANAGER_ADMIN");
        people.employee(request.employeeId()).orElseThrow(() -> bad("Employee was not found"));
        people.lockEmployee(request.employeeId());
        if (records.existsByEmployeeIdAndAttendanceDate(request.employeeId(), request.date())) throw conflict("Attendance already exists for this employee and date");
        BigDecimal hours = validateAndCalculate(request.status(), request.checkIn(), request.checkOut());
        return responses(List.of(records.save(new AttendanceRecord(request.employeeId(), request.date(), request.status(),
                request.checkIn(), request.checkOut(), hours, cleanNote(request.note()))))).getFirst();
    }

    @Transactional
    public RecordResponse correct(AccountPrincipal principal, Long id, CorrectionRequest request) {
        requireRole(principal, "MANAGER_ADMIN");
        AttendanceRecord record = records.findById(id).orElseThrow(() -> notFound("Attendance record was not found"));
        people.lockEmployee(record.getEmployeeId());
        BigDecimal hours = validateAndCalculate(request.status(), request.checkIn(), request.checkOut());
        record.correct(request.status(), request.checkIn(), request.checkOut(), hours, cleanNote(request.note()));
        return responses(List.of(records.save(record))).getFirst();
    }

    private BigDecimal validateAndCalculate(AttendanceRecord.Status status, LocalTime in, LocalTime out) {
        if (status == null) throw bad("Attendance status is required");
        boolean worked = status == AttendanceRecord.Status.PRESENT || status == AttendanceRecord.Status.LATE;
        if (!worked && (in != null || out != null)) throw bad("Absent and leave records cannot include check-in or check-out times");
        if (!worked) return BigDecimal.ZERO.setScale(2);
        if (in == null || (out != null && !out.isAfter(in))) throw bad("Worked attendance needs a check-in and any check-out must be later on the same day");
        return out == null ? BigDecimal.ZERO.setScale(2) : calculateHours(in, out);
    }

    static BigDecimal calculateHours(LocalTime in, LocalTime out) {
        long seconds = Duration.between(in, out).getSeconds();
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    private String cleanNote(String note) {
        if (note == null || note.isBlank()) return null;
        String cleaned = note.trim();
        if (cleaned.length() > 255) throw bad("Note must be at most 255 characters");
        return cleaned;
    }

    private List<RecordResponse> responses(List<AttendanceRecord> found) {
        Map<Long, String> names = new HashMap<>();
        List<RecordResponse> result = new ArrayList<>(found.size());
        for (AttendanceRecord record : found) {
            String name = names.computeIfAbsent(record.getEmployeeId(), id -> people.employee(id).map(EmployeeInfo::name).orElse("Unknown employee"));
            result.add(new RecordResponse(record.getId(), record.getEmployeeId(), name, record.getAttendanceDate(), record.getStatus(),
                    record.getCheckInTime(), record.getCheckOutTime(), record.getWorkingHours(), record.getNotes()));
        }
        return result;
    }

    private EmployeeInfo activeEmployee(Long id) {
        EmployeeInfo employee = people.employee(id).orElseThrow(() -> forbidden("Employee access is unavailable"));
        if (!employee.active()) throw forbidden("Employee is inactive");
        return employee;
    }
    private void requireRole(AccountPrincipal principal, String role) { if (!role.equals(principal.getRole())) throw forbidden("Access denied"); }
    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) throw bad("Choose a valid date range of at most 366 days");
    }
    private AttendanceModuleException bad(String message) { return new AttendanceModuleException(HttpStatus.BAD_REQUEST, message); }
    private AttendanceModuleException forbidden(String message) { return new AttendanceModuleException(HttpStatus.FORBIDDEN, message); }
    private AttendanceModuleException conflict(String message) { return new AttendanceModuleException(HttpStatus.CONFLICT, message); }
    private AttendanceModuleException notFound(String message) { return new AttendanceModuleException(HttpStatus.NOT_FOUND, message); }
}
