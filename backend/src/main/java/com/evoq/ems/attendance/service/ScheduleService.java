package com.evoq.ems.attendance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.repository.ScheduleRepository;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.attendance.web.ScheduleDtos.EntryRequest;
import com.evoq.ems.attendance.web.ScheduleDtos.EntryResponse;
import com.evoq.ems.attendance.web.AttendanceDtos.EmployeeOption;
import com.evoq.ems.attendance.web.ScheduleDtos.ScheduleResponse;
import com.evoq.ems.attendance.web.ScheduleDtos.TeamResponse;
import com.evoq.ems.attendance.web.ScheduleDtos.WriteRequest;
import com.evoq.ems.auth.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository schedules;
    private final ScheduleEntryRepository entries;
    private final AttendanceRecordRepository attendance;
    private final EmployeeTeamReader people;
    private final ApprovedLeaveReader leaves;
    private final Clock clock;

    public ScheduleService(ScheduleRepository schedules, ScheduleEntryRepository entries,
            AttendanceRecordRepository attendance, EmployeeTeamReader people, ApprovedLeaveReader leaves,
            Clock attendanceClock) {
        this.schedules = schedules;
        this.entries = entries;
        this.attendance = attendance;
        this.people = people;
        this.leaves = leaves;
        this.clock = attendanceClock;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> ownPublished(AccountPrincipal principal, LocalDate from, LocalDate to) {
        validateRange(from, to);
        requireActiveEmployee(principal.getEmployeeId());
        List<ScheduleEntry> found = entries.findEmployeeEntries(principal.getEmployeeId(), from, to, Schedule.Status.PUBLISHED);
        return groupedResponses(found);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> managedTeams(AccountPrincipal principal) {
        if (manager(principal)) return people.allTeams().stream()
                .map(team -> new TeamResponse(team.id(), team.name())).toList();
        Long teamId = authorizedTeam(principal, principal.getEmployeeId());
        return people.team(teamId).map(team -> List.of(new TeamResponse(team.id(), team.name()))).orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<EmployeeOption> teamEmployees(AccountPrincipal principal, Long teamId) {
        requireTeam(principal, teamId);
        if (manager(principal)) return people.activeEmployees().stream()
                .filter(employee -> teamId.equals(employee.teamId()))
                .map(employee -> new EmployeeOption(employee.id(), employee.name())).toList();
        return people.activeDirectReports(principal.getEmployeeId(), teamId).stream()
                .map(employee -> new EmployeeOption(employee.id(), employee.name())).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> teamSchedules(AccountPrincipal principal, Long teamId,
            LocalDate from, LocalDate to, boolean includeDrafts) {
        validateRange(from, to);
        requireTeam(principal, teamId);
        List<Schedule> found = schedules.findByTeamIdAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartAscIdAsc(
                teamId, from, to);
        if (!includeDrafts) found = found.stream().filter(s -> s.getStatus() == Schedule.Status.PUBLISHED).toList();
        Set<Long> permitted = manager(principal) ? null : people.activeDirectReportIds(principal.getEmployeeId(), teamId)
                .stream().collect(Collectors.toSet());
        return toResponses(found, entries.findByScheduleIdInOrderByWorkDateAscStartTimeAscIdAsc(
                found.stream().map(Schedule::getId).toList()).stream()
                .filter(e -> (permitted == null || permitted.contains(e.getEmployeeId()))
                        && !e.getWorkDate().isBefore(from) && !e.getWorkDate().isAfter(to))
                .toList());
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(AccountPrincipal principal, Long scheduleId) {
        Schedule schedule = schedules.findById(scheduleId).orElseThrow(() -> notFound("Schedule was not found"));
        if ("MANAGER_ADMIN".equals(principal.getRole())) return toResponses(List.of(schedule), entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId)).getFirst();
        requireTeam(principal, schedule.getTeamId());
        Set<Long> permitted = people.activeDirectReportIds(principal.getEmployeeId(), schedule.getTeamId()).stream().collect(Collectors.toSet());
        return toResponses(List.of(schedule), entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId)
                .stream().filter(e -> permitted.contains(e.getEmployeeId())).toList()).getFirst();
    }

    @Transactional
    public ScheduleResponse create(AccountPrincipal principal, WriteRequest request) {
        requireTeam(principal, request.teamId());
        validateHeader(request);
        Schedule schedule = schedules.save(new Schedule(request.teamId(), request.periodStart(), request.periodEnd()));
        replaceEntries(principal, schedule, request.entries(), request.removedEntryIds(), List.of());
        return toResponses(List.of(schedule), entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(schedule.getId())).getFirst();
    }

    // Conflict reads after the employee lock must see the preceding writer's commit,
    // even when authorization/header reads happened before waiting for that lock.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleResponse update(AccountPrincipal principal, Long scheduleId, WriteRequest request) {
        Schedule schedule = schedules.findLockedById(scheduleId).orElseThrow(() -> notFound("Schedule was not found"));
        requireTeam(principal, schedule.getTeamId());
        if (!schedule.getTeamId().equals(request.teamId())) throw forbidden("A schedule cannot be moved to another team");
        validateHeader(request);
        List<ScheduleEntry> oldEntries = entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId);
        schedule.update(request.teamId(), request.periodStart(), request.periodEnd());
        replaceEntries(principal, schedule, request.entries(), request.removedEntryIds(), oldEntries);
        return toResponses(List.of(schedule), entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId)).getFirst();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleResponse publish(AccountPrincipal principal, Long scheduleId) {
        Schedule schedule = schedules.findLockedById(scheduleId).orElseThrow(() -> notFound("Schedule was not found"));
        requireTeam(principal, schedule.getTeamId());
        List<ScheduleEntry> scheduled = entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId);
        if (schedule.getStatus() == Schedule.Status.DRAFT) {
            LocalDate businessDate = LocalDate.now(clock);
            scheduled.forEach(entry -> requireNonHistoricalDate(entry.getWorkDate(), businessDate));
        }
        validateEntries(principal, schedule, scheduled, true);
        lockEmployees(scheduled);
        validateAssignmentConflicts(scheduled, scheduleId, true);
        schedule.publish();
        return toResponses(List.of(schedule), scheduled).getFirst();
    }

    @Transactional
    public void discardDraft(AccountPrincipal principal, Long scheduleId) {
        Schedule schedule = schedules.findLockedById(scheduleId).orElseThrow(() -> notFound("Schedule was not found"));
        requireTeam(principal, schedule.getTeamId());
        if (schedule.getStatus() != Schedule.Status.DRAFT) throw conflict("Only a draft schedule can be discarded");
        List<ScheduleEntry> scheduled = entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(scheduleId);
        Set<Long> permitted = editableEmployeeIds(principal, schedule.getTeamId());
        if (!manager(principal) && scheduled.stream().anyMatch(entry -> !permitted.contains(entry.getEmployeeId()))) {
            throw forbidden("Draft includes entries outside your permitted direct reports");
        }
        entries.deleteAll(scheduled);
        schedules.delete(schedule);
    }

    private void replaceEntries(AccountPrincipal principal, Schedule schedule, List<EntryRequest> requests,
            List<Long> removalIds, List<ScheduleEntry> existing) {
        LocalDate businessDate = LocalDate.now(clock);
        java.util.stream.Stream.concat(existing.stream().map(ScheduleEntry::getEmployeeId),
                requests.stream().map(EntryRequest::employeeId).filter(java.util.Objects::nonNull))
                .distinct().sorted().forEach(people::lockEmployee);
        Set<Long> permitted = editableEmployeeIds(principal, schedule.getTeamId());
        Map<Long, ScheduleEntry> byId = existing.stream().collect(Collectors.toMap(ScheduleEntry::getId, Function.identity()));
        List<ScheduleEntry> changed = new ArrayList<>();
        Set<String> uniqueIds = new HashSet<>();
        for (EntryRequest request : requests) {
            if (request.id() != null) {
                if (!uniqueIds.add(request.id().toString())) throw bad("Schedule entry appears more than once");
                ScheduleEntry entry = byId.get(request.id());
                if (entry == null) throw bad("Entry does not belong to this schedule");
                if (!manager(principal) && !permitted.contains(entry.getEmployeeId())) throw forbidden("Entry is outside your permitted direct reports");
                if (!entry.getWorkDate().equals(request.workDate())) {
                    requireNonHistoricalDate(request.workDate(), businessDate);
                }
                if (attendance.existsByEmployeeIdAndAttendanceDate(entry.getEmployeeId(), entry.getWorkDate())
                        && assignmentChanged(entry, request)) {
                    throw conflict("An entry with attendance recorded cannot change employee, date or scheduled times");
                }
                entry.update(schedule.getId(), request.employeeId(), request.workDate(), request.startTime(), request.endTime(), clean(request.notes()));
                changed.add(entry);
            } else {
                requireNonHistoricalDate(request.workDate(), businessDate);
                changed.add(new ScheduleEntry(schedule.getId(), request.employeeId(), request.workDate(),
                        request.startTime(), request.endTime(), clean(request.notes())));
            }
        }
        List<Long> requestRemovalIds = removalIds == null ? List.of() : removalIds;
        Set<Long> removedIds = new HashSet<>();
        for (Long id : requests.stream().map(EntryRequest::id).filter(java.util.Objects::nonNull).toList()) {
            if (requestRemovalIds.contains(id)) throw bad("An entry cannot be updated and removed together");
        }
        for (Long id : requestRemovalIds) {
            if (!removedIds.add(id)) throw bad("Schedule entry removal appears more than once");
            ScheduleEntry previous = byId.get(id);
            if (previous == null) throw bad("Entry does not belong to this schedule");
            if (!manager(principal) && !permitted.contains(previous.getEmployeeId())) throw forbidden("Entry is outside your permitted direct reports");
            if (attendance.existsByEmployeeIdAndAttendanceDate(previous.getEmployeeId(), previous.getWorkDate())) {
                throw conflict("An entry with attendance recorded cannot be removed");
            }
        }
        List<ScheduleEntry> resulting = new ArrayList<>(existing.stream()
                .filter(entry -> !removedIds.contains(entry.getId())).toList());
        resulting.addAll(changed.stream().filter(entry -> entry.getId() == null).toList());
        if (schedule.getStatus() == Schedule.Status.PUBLISHED && resulting.isEmpty()) {
            throw bad("A published schedule needs at least one entry");
        }
        for (ScheduleEntry entry : resulting) {
            if (entry.getWorkDate().isBefore(schedule.getPeriodStart()) || entry.getWorkDate().isAfter(schedule.getPeriodEnd())) {
                throw bad("Schedule period cannot exclude an existing entry");
            }
        }
        validateEntries(principal, schedule, changed, false);
        validateInternalConflicts(resulting, schedule.getStatus() == Schedule.Status.PUBLISHED);
        validateAssignmentConflicts(changed, schedule.getId(), schedule.getStatus() == Schedule.Status.PUBLISHED);
        entries.deleteAll(existing.stream().filter(entry -> removedIds.contains(entry.getId())).toList());
        entries.saveAll(changed);
    }

    private void validateInternalConflicts(List<ScheduleEntry> values, boolean publishing) {
        for (int i = 0; i < values.size(); i++) {
            ScheduleEntry current = values.get(i);
            for (int j = 0; j < i; j++) {
                ScheduleEntry prior = values.get(j);
                if (!prior.getEmployeeId().equals(current.getEmployeeId()) || !prior.getWorkDate().equals(current.getWorkDate())) continue;
                if (publishing) throw conflict("V1 supports one published schedule entry per employee per day");
                if (prior.getStartTime().isBefore(current.getEndTime()) && prior.getEndTime().isAfter(current.getStartTime())) {
                    throw conflict("Employee has overlapping schedule entries");
                }
            }
        }
    }

    private void validateEntries(AccountPrincipal principal, Schedule schedule, List<ScheduleEntry> values, boolean publishing) {
        if (values.isEmpty() && publishing) throw bad("A schedule needs at least one entry before publishing");
        Set<String> seen = new HashSet<>();
        Map<Long, EmployeeInfo> validated = new java.util.HashMap<>();
        Set<Long> permittedEmployees = editableEmployeeIds(principal, schedule.getTeamId());
        for (ScheduleEntry entry : values) {
            if (entry.getEmployeeId() == null || entry.getWorkDate() == null || entry.getStartTime() == null || entry.getEndTime() == null) {
                throw bad("Employee, work date and shift times are required");
            }
            if (entry.getWorkDate().isBefore(schedule.getPeriodStart()) || entry.getWorkDate().isAfter(schedule.getPeriodEnd())) {
                throw bad("Every work date must fall inside the schedule period");
            }
            if (!entry.getEndTime().isAfter(entry.getStartTime())) throw bad("Shift end time must be after start time on the same day");
            EmployeeInfo employee = validated.computeIfAbsent(entry.getEmployeeId(), id -> people.employee(id)
                    .orElseThrow(() -> bad("Employee was not found")));
            if (!employee.active()) throw bad("Only active employees can be scheduled");
            if (!schedule.getTeamId().equals(employee.teamId())) throw bad("Employee must belong to the scheduled team");
            if (!permittedEmployees.contains(employee.id())) throw forbidden("Employee is outside your permitted direct reports");
            String dayKey = entry.getEmployeeId() + ":" + entry.getWorkDate();
            if (!seen.add(dayKey) && publishing) throw conflict("V1 supports one published schedule entry per employee per day");
        }
    }

    private void validateAssignmentConflicts(List<ScheduleEntry> values, Long scheduleId, boolean publishing) {
        for (int i = 0; i < values.size(); i++) {
            ScheduleEntry current = values.get(i);
            for (int j = 0; j < i; j++) {
                ScheduleEntry prior = values.get(j);
                if (prior.getEmployeeId().equals(current.getEmployeeId())
                        && prior.getWorkDate().equals(current.getWorkDate())
                        && prior.getStartTime().isBefore(current.getEndTime())
                        && prior.getEndTime().isAfter(current.getStartTime())) {
                    throw conflict("Employee has overlapping schedule entries");
                }
            }
            if (publishing && scheduleId != null && entries.countOverlapsOutsideSchedule(current.getEmployeeId(), current.getWorkDate(),
                    scheduleId, current.getStartTime(), current.getEndTime()) > 0) {
                throw conflict("Employee has an overlapping schedule entry");
            }
            if (publishing && entries.countEntriesOutsideSchedule(current.getEmployeeId(), current.getWorkDate(),
                    scheduleId, Schedule.Status.PUBLISHED) > 0) {
                throw conflict("V1 supports one published schedule entry per employee per day");
            }
            if (!leaves.approvedConflicts(current.getEmployeeId(), current.getWorkDate(), current.getWorkDate()).isEmpty()) {
                throw conflict("Employee has approved leave on " + current.getWorkDate());
            }
        }
    }

    private boolean assignmentChanged(ScheduleEntry current, EntryRequest request) {
        return !current.getEmployeeId().equals(request.employeeId()) || !current.getWorkDate().equals(request.workDate())
                || !current.getStartTime().equals(request.startTime()) || !current.getEndTime().equals(request.endTime());
    }

    private void requireNonHistoricalDate(LocalDate workDate, LocalDate businessDate) {
        if (workDate != null && workDate.isBefore(businessDate)) {
            throw bad("New or rescheduled shifts and draft publication require a work date on or after the business date " + businessDate);
        }
    }

    private void lockEmployees(List<ScheduleEntry> values) {
        values.stream().map(ScheduleEntry::getEmployeeId).distinct().sorted().forEach(people::lockEmployee);
    }

    private List<ScheduleResponse> groupedResponses(List<ScheduleEntry> values) {
        Map<Long, List<ScheduleEntry>> grouped = values.stream().collect(Collectors.groupingBy(
                ScheduleEntry::getScheduleId, java.util.LinkedHashMap::new, Collectors.toList()));
        List<Schedule> found = grouped.keySet().stream().map(id -> schedules.findById(id).orElseThrow()).toList();
        return toResponses(found, values);
    }

    private List<ScheduleResponse> toResponses(List<Schedule> schedulesFound, List<ScheduleEntry> values) {
        Map<Long, List<EntryResponse>> bySchedule = values.stream().collect(Collectors.groupingBy(ScheduleEntry::getScheduleId,
                java.util.LinkedHashMap::new, Collectors.mapping(this::entryResponse, Collectors.toList())));
        List<ScheduleResponse> result = new ArrayList<>();
        for (Schedule schedule : schedulesFound) {
            String teamName = people.team(schedule.getTeamId()).map(EmployeeTeamReader.TeamInfo::name).orElse("Unknown team");
            result.add(new ScheduleResponse(schedule.getId(), schedule.getTeamId(), teamName,
                    schedule.getPeriodStart(), schedule.getPeriodEnd(), schedule.getStatus(),
                    bySchedule.getOrDefault(schedule.getId(), List.of())));
        }
        return result;
    }

    private EntryResponse entryResponse(ScheduleEntry entry) {
        String name = people.employee(entry.getEmployeeId()).map(EmployeeInfo::name).orElse("Unknown employee");
        return new EntryResponse(entry.getId(), entry.getEmployeeId(), name, entry.getWorkDate(),
                entry.getStartTime(), entry.getEndTime(), entry.getNotes());
    }

    private Long authorizedTeam(AccountPrincipal principal, Long employeeId) {
        requireRole(principal, "SUPERVISOR");
        EmployeeInfo supervisor = requireActiveEmployee(employeeId);
        if (supervisor.teamId() == null || people.activeDirectReportIds(supervisor.id(), supervisor.teamId()).isEmpty()) {
            throw forbidden("No permitted scheduling team is assigned to you");
        }
        return supervisor.teamId();
    }

    private void requireTeam(AccountPrincipal principal, Long teamId) {
        if (manager(principal)) {
            if (people.team(teamId).isEmpty()) throw notFound("Team was not found");
            return;
        }
        if (!"SUPERVISOR".equals(principal.getRole())) throw forbidden("Access denied");
        if (!authorizedTeam(principal, principal.getEmployeeId()).equals(teamId)) throw forbidden("This team is not assigned to you");
    }

    private EmployeeInfo requireActiveEmployee(Long id) {
        EmployeeInfo employee = people.employee(id).orElseThrow(() -> forbidden("Employee access is unavailable"));
        if (!employee.active()) throw forbidden("Employee is inactive");
        return employee;
    }

    private void validateHeader(WriteRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) throw bad("Schedule end must be on or after its start");
        if (request.entries() == null) throw bad("Schedule entries are required");
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.length() > 255) throw bad("Entry notes must be at most 255 characters");
        return result;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) throw bad("Choose a valid date range of at most 366 days");
    }

    private AttendanceModuleException bad(String message) { return new AttendanceModuleException(HttpStatus.BAD_REQUEST, message); }
    private AttendanceModuleException forbidden(String message) { return new AttendanceModuleException(HttpStatus.FORBIDDEN, message); }
    private AttendanceModuleException conflict(String message) { return new AttendanceModuleException(HttpStatus.CONFLICT, message); }
    private AttendanceModuleException notFound(String message) { return new AttendanceModuleException(HttpStatus.NOT_FOUND, message); }
    private boolean manager(AccountPrincipal principal) { return "MANAGER_ADMIN".equals(principal.getRole()); }
    private Set<Long> editableEmployeeIds(AccountPrincipal principal, Long teamId) {
        if (manager(principal)) return people.activeEmployees().stream()
                .filter(employee -> teamId.equals(employee.teamId()))
                .map(EmployeeInfo::id).collect(Collectors.toSet());
        return people.activeDirectReportIds(principal.getEmployeeId(), teamId).stream().collect(Collectors.toSet());
    }
    private void requireRole(AccountPrincipal principal, String role) { if (!role.equals(principal.getRole())) throw forbidden("Access denied"); }
}
