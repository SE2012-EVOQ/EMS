package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.EmployeeInfo;
import com.evoq.ems.attendance.integration.EmployeeTeamReader.TeamInfo;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import com.evoq.ems.attendance.repository.ScheduleRepository;
import com.evoq.ems.attendance.service.ScheduleService;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.attendance.web.ScheduleDtos.EntryRequest;
import com.evoq.ems.attendance.web.ScheduleDtos.WriteRequest;
import com.evoq.ems.auth.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ScheduleServiceTests {
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    private final AttendanceRecordRepository attendance = mock(AttendanceRecordRepository.class);
    private final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    private final ApprovedLeaveReader leaves = mock(ApprovedLeaveReader.class);
    private final ScheduleService service = new ScheduleService(schedules, entries, attendance, people, leaves);
    private final AccountPrincipal supervisor = mock(AccountPrincipal.class);
    private final LocalDate day = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setup() {
        when(supervisor.getEmployeeId()).thenReturn(2L);
        when(supervisor.getRole()).thenReturn("SUPERVISOR");
        when(people.employee(2L)).thenReturn(Optional.of(new EmployeeInfo(2L, "Demo Supervisor", 7L, null, "ACTIVE")));
        when(people.employee(3L)).thenReturn(Optional.of(new EmployeeInfo(3L, "Demo Employee", 7L, 2L, "ACTIVE")));
        when(people.activeDirectReportIds(2L, 7L)).thenReturn(List.of(3L));
        when(people.team(7L)).thenReturn(Optional.of(new TeamInfo(7L, "Development Scheduling Team")));
        when(leaves.approvedConflicts(3L, day, day)).thenReturn(List.of());
        when(entries.countOverlapsOutsideSchedule(3L, day, 41L, LocalTime.of(9, 0), LocalTime.of(17, 0))).thenReturn(0L);
    }

    @Test
    void createDraftAcceptsActiveDirectReportInAuthorizedTeam() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(schedules.findById(41L)).thenReturn(Optional.of(schedule));
        when(entries.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of());

        var result = service.create(supervisor, new WriteRequest(7L, day, day,
                List.of(new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))));

        assertEquals(Schedule.Status.DRAFT, result.status());
        verify(people).lockEmployee(3L);
        verify(leaves).approvedConflicts(3L, day, day);
    }

    @Test
    void rejectsWrongTeamAndNonDirectReport() {
        AttendanceModuleException team = assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(8L, day, day, List.of())));
        assertEquals(HttpStatus.FORBIDDEN, team.status());

        when(people.employee(4L)).thenReturn(Optional.of(new EmployeeInfo(4L, "Other employee", 7L, 9L, "ACTIVE")));
        Schedule schedule = new Schedule(7L, day, day);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        AttendanceModuleException denied = assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 4L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null)))));
        assertEquals(HttpStatus.FORBIDDEN, denied.status());
    }

    @Test
    void rejectsInvalidTimeAndApprovedLeaveConflict() {
        Schedule schedule = new Schedule(7L, day, day);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 3L, day, LocalTime.of(17, 0), LocalTime.of(9, 0), null)))));

        when(leaves.approvedConflicts(3L, day, day)).thenReturn(List.of(new ApprovedLeaveReader.LeaveConflict(day, day)));
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))))).status());
    }

    @Test
    void entryMustFallInsideSchedulePeriod() {
        Schedule schedule = new Schedule(7L, day, day);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 3L, day.plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), null))))).status());
    }

    @Test
    void scheduleAssignmentWithAttendanceCannotBeChanged() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry existing = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ReflectionTestUtils.setField(existing, "id", 51L);
        when(schedules.findById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(existing));
        when(attendance.existsByEmployeeIdAndAttendanceDate(3L, day)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> service.update(supervisor, 41L,
                new WriteRequest(7L, day, day, List.of(
                        new EntryRequest(51L, 3L, day, LocalTime.of(10, 0), LocalTime.of(18, 0), null))))).status());
    }

    @Test
    void rejectsAnOverlappingEntryInAnotherSchedule() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(entries.countOverlapsOutsideSchedule(3L, day, 41L, LocalTime.of(9, 0), LocalTime.of(17, 0))).thenReturn(1L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))))).status());
    }

    @Test
    void draftMayHaveMultipleNonOverlappingEntriesButPublishRejectsMultiplePublishedOccurrences() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(schedules.findById(41L)).thenReturn(Optional.of(schedule));
        var first = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(12, 0), null);
        var second = new ScheduleEntry(41L, 3L, day, LocalTime.of(12, 0), LocalTime.of(17, 0), null);
        when(entries.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(first, second));
        service.create(supervisor, new WriteRequest(7L, day, day, List.of(
                new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(12, 0), null),
                new EntryRequest(null, 3L, day, LocalTime.of(12, 0), LocalTime.of(17, 0), null))));
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class,
                () -> service.publish(supervisor, 41L)).status());
    }
}
