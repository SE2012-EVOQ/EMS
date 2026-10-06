package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
import org.mockito.ArgumentCaptor;

class ScheduleServiceTests {
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final ScheduleEntryRepository entries = mock(ScheduleEntryRepository.class);
    private final AttendanceRecordRepository attendance = mock(AttendanceRecordRepository.class);
    private final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    private final ApprovedLeaveReader leaves = mock(ApprovedLeaveReader.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-30T03:30:00Z"), ZoneId.of("Asia/Colombo"));
    private final ScheduleService service = new ScheduleService(schedules, entries, attendance, people, leaves, clock);
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
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of());

        var result = service.create(supervisor, new WriteRequest(7L, day, day,
                List.of(new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))));

        assertEquals(Schedule.Status.DRAFT, result.status());
        verify(people).lockEmployee(3L);
        verify(leaves).approvedConflicts(3L, day, day);
    }

    @Test
    void newShiftCannotBeCreatedBeforeColomboBusinessDateEvenWhileUtcIsYesterday() {
        Clock boundary = Clock.fixed(Instant.parse("2026-09-29T18:35:00Z"), ZoneId.of("Asia/Colombo"));
        ScheduleService business = new ScheduleService(schedules, entries, attendance, people, leaves, boundary);
        Schedule schedule = new Schedule(7L, day.minusDays(1), day);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);

        var rejected = assertThrows(AttendanceModuleException.class, () -> business.create(supervisor,
                new WriteRequest(7L, day.minusDays(1), day, List.of(new EntryRequest(null, 3L,
                        day.minusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), null)))));

        assertEquals(HttpStatus.BAD_REQUEST, rejected.status());
        assertEquals(true, rejected.getMessage().contains(day.toString()));
        verify(entries, never()).saveAll(any());
    }

    @Test
    void addingNewHistoricalEntryToExistingScheduleIsRejected() {
        Schedule schedule = existingSchedule(day.minusDays(2), day, true);
        ScheduleEntry historical = existingEntry(day.minusDays(2));
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(historical));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AttendanceModuleException.class,
                () -> service.update(supervisor, 41L, new WriteRequest(7L, day.minusDays(2), day,
                        List.of(new EntryRequest(null, 3L, day.minusDays(1), LocalTime.of(9, 0),
                                LocalTime.of(17, 0), null))))).status());
        assertEquals(day.minusDays(2), historical.getWorkDate());
        verify(entries, never()).saveAll(any());
        verify(entries, never()).deleteAll(any());
    }

    @Test
    void existingIdCannotBypassBackdatingProtectionByMovingToAnotherPastDate() {
        Schedule schedule = existingSchedule(day.minusDays(2), day, true);
        ScheduleEntry existing = existingEntry(day);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(existing));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AttendanceModuleException.class,
                () -> service.update(supervisor, 41L, new WriteRequest(7L, day.minusDays(2), day,
                        List.of(new EntryRequest(51L, 3L, day.minusDays(1), LocalTime.of(9, 0),
                                LocalTime.of(17, 0), null))))).status());
        assertEquals(day, existing.getWorkDate());
        verify(entries, never()).saveAll(any());
    }

    @Test
    void retainingAnExistingHistoricalDateIsAllowedAndDoesNotRewriteIt() {
        LocalDate historicalDate = day.minusDays(2);
        Schedule schedule = existingSchedule(historicalDate, day, true);
        ScheduleEntry existing = existingEntry(historicalDate);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(existing));

        var updated = service.update(supervisor, 41L, new WriteRequest(7L, historicalDate, day,
                List.of(new EntryRequest(51L, 3L, historicalDate, LocalTime.of(9, 0), LocalTime.of(17, 0), "Note"))));

        assertEquals(historicalDate, existing.getWorkDate());
        assertEquals(historicalDate, updated.entries().getFirst().workDate());
        assertEquals("Note", existing.getNotes());
    }

    @Test
    void draftPublicationRejectsHistoricalDatesWithoutChangingTheDraft() {
        LocalDate historicalDate = day.minusDays(1);
        Schedule schedule = existingSchedule(historicalDate, historicalDate, false);
        ScheduleEntry historical = existingEntry(historicalDate);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(historical));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AttendanceModuleException.class,
                () -> service.publish(supervisor, 41L)).status());
        assertEquals(Schedule.Status.DRAFT, schedule.getStatus());
        assertEquals(historicalDate, historical.getWorkDate());
        verify(entries, never()).saveAll(any());
        verify(entries, never()).deleteAll(any());
    }

    @Test
    void draftThatWasValidYesterdayCannotPublishAfterBusinessMidnight() {
        Schedule schedule = existingSchedule(day, day, false);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(existingEntry(day)));
        Clock tomorrow = Clock.fixed(Instant.parse("2026-09-30T18:30:00Z"), ZoneId.of("Asia/Colombo"));
        ScheduleService business = new ScheduleService(schedules, entries, attendance, people, leaves, tomorrow);

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AttendanceModuleException.class,
                () -> business.publish(supervisor, 41L)).status());
        assertEquals(Schedule.Status.DRAFT, schedule.getStatus());
    }

    @Test
    void publishedHistoricalScheduleRemainsReadableAndIsNotSubjectToDraftDateValidation() {
        LocalDate historicalDate = day.minusDays(10);
        Schedule schedule = existingSchedule(historicalDate, historicalDate, true);
        when(schedules.findById(41L)).thenReturn(Optional.of(schedule));
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L))
                .thenReturn(List.of(existingEntry(historicalDate)));

        assertEquals(historicalDate, service.get(supervisor, 41L).entries().getFirst().workDate());
        assertEquals(Schedule.Status.PUBLISHED, service.publish(supervisor, 41L).status());
        assertEquals(historicalDate, service.get(supervisor, 41L).entries().getFirst().workDate());
        verify(entries, never()).saveAll(any());
        verify(entries, never()).deleteAll(any());
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
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(existing));
        when(attendance.existsByEmployeeIdAndAttendanceDate(3L, day)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> service.update(supervisor, 41L,
                new WriteRequest(7L, day, day, List.of(
                        new EntryRequest(51L, 3L, day, LocalTime.of(10, 0), LocalTime.of(18, 0), null))))).status());
    }

    @Test
    void omittedEntriesRemainWhenUpdatingAFilteredSchedule() {
        Schedule schedule = new Schedule(7L, day, day.plusDays(30));
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry visible = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ScheduleEntry outsideDateWindow = new ScheduleEntry(41L, 3L, day.plusDays(30), LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ScheduleEntry anotherSupervisors = new ScheduleEntry(41L, 4L, day.plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ReflectionTestUtils.setField(visible, "id", 51L);
        ReflectionTestUtils.setField(outsideDateWindow, "id", 52L);
        ReflectionTestUtils.setField(anotherSupervisors, "id", 53L);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L))
                .thenReturn(List.of(visible, outsideDateWindow, anotherSupervisors));
        when(people.employee(4L)).thenReturn(Optional.of(new EmployeeInfo(4L, "Other report", 7L, 9L, "ACTIVE")));

        service.update(supervisor, 41L, new WriteRequest(7L, day, day.plusDays(30),
                List.of(new EntryRequest(51L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))));

        ArgumentCaptor<List<ScheduleEntry>> deleted = ArgumentCaptor.forClass(List.class);
        verify(entries).deleteAll(deleted.capture());
        assertEquals(List.of(), deleted.getValue());
    }

    @Test
    void explicitRemovalCannotTargetAnotherSupervisorsEntry() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry protectedEntry = new ScheduleEntry(41L, 4L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ReflectionTestUtils.setField(protectedEntry, "id", 53L);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(protectedEntry));

        AttendanceModuleException denied = assertThrows(AttendanceModuleException.class,
                () -> service.update(supervisor, 41L,
                        new WriteRequest(7L, day, day, List.of(), List.of(53L))));

        assertEquals(HttpStatus.FORBIDDEN, denied.status());
        verify(entries, never()).deleteAll(any());
    }

    @Test
    void explicitRemovalDeletesOnlySelectedPermittedEntry() {
        Schedule schedule = new Schedule(7L, day, day.plusDays(1));
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry removed = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ScheduleEntry retained = new ScheduleEntry(41L, 3L, day.plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ReflectionTestUtils.setField(removed, "id", 51L);
        ReflectionTestUtils.setField(retained, "id", 52L);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(removed, retained));

        service.update(supervisor, 41L,
                new WriteRequest(7L, day, day.plusDays(1), List.of(), List.of(51L)));

        ArgumentCaptor<List<ScheduleEntry>> deleted = ArgumentCaptor.forClass(List.class);
        verify(entries).deleteAll(deleted.capture());
        assertEquals(List.of(removed), deleted.getValue());
    }

    @Test
    void draftMayOverlapAnotherScheduleButPublishingRejectsPublishedConflicts() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        ScheduleEntry shift = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(shift));
        when(entries.countOverlapsOutsideSchedule(3L, day, 41L, LocalTime.of(9, 0), LocalTime.of(17, 0))).thenReturn(1L);
        assertEquals(Schedule.Status.DRAFT, service.create(supervisor,
                new WriteRequest(7L, day, day,
                        List.of(new EntryRequest(null, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null)))).status());
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(AttendanceModuleException.class, () -> service.publish(supervisor, 41L)).status());
    }

    @Test
    void supervisorCanDiscardOwnDraft() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry entry = new ScheduleEntry(41L, 3L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(entry));

        service.discardDraft(supervisor, 41L);

        verify(entries).deleteAll(List.of(entry));
        verify(schedules).delete(schedule);
    }

    @Test
    void supervisorCannotDiscardDraftContainingAnotherSupervisorsEntry() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry protectedEntry = new ScheduleEntry(41L, 4L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(protectedEntry));

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(AttendanceModuleException.class, () -> service.discardDraft(supervisor, 41L)).status());
        verify(schedules, never()).delete(any());
    }

    @Test
    void managerCanManageTeamsAndDiscardAnOrphanedDraft() {
        AccountPrincipal manager = mock(AccountPrincipal.class);
        when(manager.getRole()).thenReturn("MANAGER_ADMIN");
        when(people.allTeams()).thenReturn(List.of(new TeamInfo(7L, "Team Seven")));
        when(people.team(7L)).thenReturn(Optional.of(new TeamInfo(7L, "Team Seven")));
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        ScheduleEntry orphaned = new ScheduleEntry(41L, 4L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(orphaned));

        assertEquals(List.of(new com.evoq.ems.attendance.web.ScheduleDtos.TeamResponse(7L, "Team Seven")),
                service.managedTeams(manager));
        service.discardDraft(manager, 41L);
        verify(schedules).delete(schedule);
    }

    @Test
    void managerCanScheduleAnActiveSupervisorInTheirTeam() {
        AccountPrincipal manager = mock(AccountPrincipal.class);
        when(manager.getRole()).thenReturn("MANAGER_ADMIN");
        when(people.activeEmployees()).thenReturn(List.of(
                new EmployeeInfo(2L, "Demo Supervisor", 7L, null, "ACTIVE")));
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(entries.findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(41L)).thenReturn(List.of(
                new ScheduleEntry(41L, 2L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null)));

        var created = service.create(manager, new WriteRequest(7L, day, day,
                List.of(new EntryRequest(null, 2L, day, LocalTime.of(9, 0), LocalTime.of(17, 0), null))));

        assertEquals(Schedule.Status.DRAFT, created.status());
        assertEquals(2L, created.entries().getFirst().employeeId());
        verify(people).lockEmployee(2L);
    }

    @Test
    void draftMayHaveMultipleNonOverlappingEntriesButPublishRejectsMultiplePublishedOccurrences() {
        Schedule schedule = new Schedule(7L, day, day);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        when(schedules.save(any(Schedule.class))).thenReturn(schedule);
        when(schedules.findLockedById(41L)).thenReturn(Optional.of(schedule));
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
    private Schedule existingSchedule(LocalDate start, LocalDate end, boolean published) {
        Schedule schedule = new Schedule(7L, start, end);
        ReflectionTestUtils.setField(schedule, "id", 41L);
        if (published) schedule.publish();
        return schedule;
    }

    private ScheduleEntry existingEntry(LocalDate workDate) {
        ScheduleEntry entry = new ScheduleEntry(41L, 3L, workDate, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        ReflectionTestUtils.setField(entry, "id", 51L);
        return entry;
    }
}
