package com.evoq.ems.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.service.*;
import com.evoq.ems.attendance.web.AttendanceDtos.RecordResponse;
import com.evoq.ems.attendance.web.AttendanceModuleException;
import com.evoq.ems.attendance.web.ScheduleDtos.*;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.leave.web.LeaveDtos.SubmitLeaveRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Separate real MySQL transactions, committed isolated fixtures, and explicit cleanup.
 * The spy supplies a barrier only; every employee lock still executes its real SQL. */
@SpringBootTest
@ActiveProfiles("dev")
@Import(AttendanceConcurrencyMySqlTests.TimeConfiguration.class)
class AttendanceConcurrencyMySqlTests {
    @Autowired ScheduleService schedules;
    @Autowired AttendanceService attendance;
    @Autowired AttendanceReconciliationService reconciliation;
    @Autowired LeaveService leave;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired TestClock clock;
    @MockitoSpyBean EmployeeTeamReader people;
    @MockitoBean AttendanceReconciliationJob backgroundJob;

    private final LocalDate day = LocalDate.of(2099, 6, 15);
    private Long departmentId, teamId, supervisorId, employeeId, leaveTypeId;
    private AccountPrincipal employee, supervisor;
    private ExecutorService workers;

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary TestClock verificationClock() { return new TestClock(); }
    }

    static class TestClock extends Clock {
        private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.parse("2099-06-15T03:35:06Z"));
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Colombo"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return instant.get(); }
        void at(LocalDate date, LocalTime time) { instant.set(date.atTime(time).atZone(getZone()).toInstant()); }
    }

    @BeforeEach
    void fixtures() {
        clock.at(day, LocalTime.of(9, 5, 6));
        String tag = "AS08-" + UUID.randomUUID();
        jdbc.update("INSERT INTO department(name) VALUES (?)", tag);
        departmentId = jdbc.queryForObject("SELECT department_id FROM department WHERE name=?", Long.class, tag);
        jdbc.update("INSERT INTO team_project(name) VALUES (?)", tag);
        teamId = jdbc.queryForObject("SELECT team_id FROM team_project WHERE name=?", Long.class, tag);
        supervisorId = insertEmployee(tag + "-supervisor@example.invalid", null);
        employeeId = insertEmployee(tag + "-employee@example.invalid", supervisorId);
        supervisor = principal(supervisorId, "SUPERVISOR");
        employee = principal(employeeId, "EMPLOYEE");
        jdbc.update("INSERT INTO leave_type(name) VALUES (?)", tag);
        leaveTypeId = jdbc.queryForObject("SELECT leave_type_id FROM leave_type WHERE name=?", Long.class, tag);
        jdbc.update("INSERT INTO leave_balance(employee_id,leave_type_id,available_days,used_days) VALUES (?,?,25,0)", employeeId, leaveTypeId);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        if (workers != null) {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "Workers must stop before deleting their fixtures");
        }
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            if (departmentId == null) return;
            jdbc.update("DELETE FROM leave_request WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
            jdbc.update("DELETE FROM leave_balance WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
            jdbc.update("DELETE FROM attendance_record WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
            jdbc.update("DELETE FROM schedule_entry WHERE schedule_id IN (SELECT schedule_id FROM schedule WHERE team_id=?)", teamId);
            jdbc.update("DELETE FROM schedule WHERE team_id=?", teamId);
            jdbc.update("DELETE FROM employee WHERE department_id=?", departmentId);
            jdbc.update("DELETE FROM team_project WHERE team_id=?", teamId);
            jdbc.update("DELETE FROM department WHERE department_id=?", departmentId);
            if (leaveTypeId != null) jdbc.update("DELETE FROM leave_type WHERE leave_type_id=?", leaveTypeId);
        });
    }

    @Test
    void competingCheckInsCommitExactlyOneAttendanceRow() throws Exception {
        publish(day);
        var outcomes = compete(() -> attendance.checkIn(employee), () -> attendance.checkIn(employee));
        assertOneSuccessOneConflict(outcomes);
        assertEquals(1, attendanceCount(day));
        assertEquals("09:05:06", jdbc.queryForObject("SELECT CAST(check_in_time AS CHAR) FROM attendance_record WHERE employee_id=? AND attendance_date=?", String.class, employeeId, day));
    }

    @ParameterizedTest
    @CsvSource({"16:40:13,7.59", "17:00:00,7.92", "17:20:37,7.92"})
    void manualWinnerBeforeAtOrAfterEndSurvivesRepeatedReconciliation(String time, String hours) {
        openAttendance();
        clock.at(day, LocalTime.parse(time));
        RecordResponse result = attendance.checkOut(employee);
        reconciliation.reconcile(employeeId, day, day.atTime(17, 30));
        reconciliation.reconcile(employeeId, day, day.atTime(18, 0));
        assertStableCompletion(time, hours);
        assertEquals(LocalTime.parse(time), result.checkOut());
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> attendance.checkOut(employee)).status());
    }

    @Test
    void reconciliationWinnerClosesAtScheduledEndAndManualCheckoutConflicts() {
        openAttendance();
        clock.at(day, LocalTime.of(17, 20, 37));
        reconciliation.reconcile(employeeId, day, day.atTime(17, 0));
        reconciliation.reconcile(employeeId, day, day.atTime(17, 30));
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> attendance.checkOut(employee)).status());
        assertStableCompletion("17:00:00", "7.92");
    }

    @ParameterizedTest
    @ValueSource(strings = {"17:00:00", "17:20:37"})
    void competingCheckoutAndReconciliationProduceOneStableCompletion(String time) throws Exception {
        openAttendance();
        clock.at(day, LocalTime.parse(time));
        var outcomes = compete(() -> attendance.checkOut(employee), () -> {
            reconciliation.reconcile(employeeId, day, day.atTime(LocalTime.parse(time)));
            return null;
        });
        assertNull(outcomes.get(1).error());
        Outcome manual = outcomes.getFirst();
        String winnerTime;
        if (manual.error() == null) {
            winnerTime = time;
            assertEquals(LocalTime.parse(time), ((RecordResponse) manual.value()).checkOut());
        } else {
            assertEquals(HttpStatus.CONFLICT, ((AttendanceModuleException) manual.error()).status());
            winnerTime = "17:00:00";
        }
        reconciliation.reconcile(employeeId, day, day.atTime(18, 0));
        reconciliation.reconcile(employeeId, day, day.atTime(18, 1));
        assertStableCompletion(winnerTime, "7.92");
        assertEquals(HttpStatus.CONFLICT, assertThrows(AttendanceModuleException.class, () -> attendance.checkOut(employee)).status());
    }

    @Test
    void competingDraftPublicationsCannotPublishTwoShiftsForOneEmployeeDate() throws Exception {
        long first = draft(day.plusDays(10)).id();
        long second = draft(day.plusDays(10)).id();
        var outcomes = compete(() -> schedules.publish(supervisor, first), () -> schedules.publish(supervisor, second));
        assertOneSuccessOneConflict(outcomes);
        assertEquals(1, publishedCount(day.plusDays(10)));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM schedule WHERE team_id=? AND status='DRAFT'", Integer.class, teamId));
    }

    @Test
    void competingPublishedUpdateAndDraftPublicationCannotIntroduceSecondShift() throws Exception {
        var moving = draft(day.plusDays(11));
        schedules.publish(supervisor, moving.id());
        var contender = draft(day.plusDays(10));
        var entry = moving.entries().getFirst();
        var request = new WriteRequest(teamId, day.plusDays(10), day.plusDays(11), List.of(
                new EntryRequest(entry.id(), employeeId, day.plusDays(10), LocalTime.of(9, 0), LocalTime.of(17, 0), "AS08 changed shift")));
        var outcomes = compete(() -> schedules.update(supervisor, moving.id(), request),
                () -> schedules.publish(supervisor, contender.id()));
        assertOneSuccessOneConflict(outcomes);
        assertEquals(1, publishedCount(day.plusDays(10)));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM schedule_entry WHERE employee_id=?", Integer.class, employeeId));
    }

    @Test
    void actualLeaveApprovalUpdatesRequestAndBalanceAndRemovesOnlyFuturePublishedShifts() {
        publish(day.plusDays(1));
        publish(day.plusDays(2));
        publish(day.plusDays(3));
        publish(day.plusDays(4));
        var pending = leave.submit(employeeId, new SubmitLeaveRequest(leaveTypeId, day.plusDays(1), day.plusDays(3), "AS08 full approval"));
        assertEquals("PENDING", pending.status());
        assertBalance("25.00", "0.00");

        var approved = leave.approve(pending.id(), supervisor);

        assertEquals("APPROVED", approved.status());
        assertEquals(3, approved.days());
        assertEquals("APPROVED", requestStatus(pending.id()));
        assertBalance("22.00", "3.00");
        for (int offset = 1; offset <= 3; offset++) {
            assertEquals(0, publishedCount(day.plusDays(offset)));
            assertEquals(0, attendanceCount(day.plusDays(offset)), "Approval must not insert LEAVE attendance");
        }
        assertEquals(1, publishedCount(day.plusDays(4)));
    }

    @Test
    void attendanceProtectedFutureShiftRejectsApprovalWithoutChangingRequestBalanceOrShifts() {
        publish(day.plusDays(1));
        publish(day.plusDays(2));
        var pending = leave.submit(employeeId, new SubmitLeaveRequest(leaveTypeId, day.plusDays(1), day.plusDays(2), "AS08 blocked approval"));
        jdbc.update("INSERT INTO attendance_record(employee_id,attendance_date,status,working_hours) VALUES (?,?,'ABSENT',0)", employeeId, day.plusDays(2));

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> leave.approve(pending.id(), supervisor)).getStatusCode());

        assertEquals("PENDING", requestStatus(pending.id()));
        assertBalance("25.00", "0.00");
        assertEquals(1, publishedCount(day.plusDays(1)));
        assertEquals(1, publishedCount(day.plusDays(2)));
        assertEquals(1, attendanceCount(day.plusDays(2)));
    }

    @Test
    void failureAfterShiftDeletionRollsBackDeletionAndTheEntireLeaveDecision() {
        publish(day.plusDays(1));
        publish(day.plusDays(2));
        var pending = leave.submit(employeeId, new SubmitLeaveRequest(leaveTypeId, day.plusDays(1), day.plusDays(2), "AS08 rollback"));
        jdbc.update("UPDATE leave_balance SET available_days=1 WHERE employee_id=? AND leave_type_id=?", employeeId, leaveTypeId);

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> leave.approve(pending.id(), supervisor)).getStatusCode());

        assertEquals("PENDING", requestStatus(pending.id()));
        assertBalance("1.00", "0.00");
        assertEquals(1, publishedCount(day.plusDays(1)));
        assertEquals(1, publishedCount(day.plusDays(2)));
    }

    private List<Outcome> compete(Supplier<?> first, Supplier<?> second) throws Exception {
        CountDownLatch arrived = new CountDownLatch(2), release = new CountDownLatch(1);
        doAnswer(call -> {
            if (Thread.currentThread().getName().startsWith("as08-race")) {
                arrived.countDown();
                assertTrue(release.await(15, TimeUnit.SECONDS), "Timed out waiting for competitor");
            }
            return call.callRealMethod();
        }).when(people).lockEmployee(employeeId);
        workers = Executors.newFixedThreadPool(2, task -> new Thread(task, "as08-race-" + UUID.randomUUID()));
        Future<Outcome> a = workers.submit(() -> outcome(first));
        Future<Outcome> b = workers.submit(() -> outcome(second));
        try {
            assertTrue(arrived.await(15, TimeUnit.SECONDS), "Both real transactions must reach the employee lock");
        } finally { release.countDown(); }
        return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
    }

    private Outcome outcome(Supplier<?> operation) {
        try { return new Outcome(operation.get(), null); }
        catch (Exception error) { return new Outcome(null, error); }
    }
    private record Outcome(Object value, Exception error) { }

    private void assertOneSuccessOneConflict(List<Outcome> outcomes) {
        assertEquals(1, outcomes.stream().filter(value -> value.error() == null).count(), outcomes.toString());
        var error = outcomes.stream().filter(value -> value.error() != null).findFirst().orElseThrow().error();
        assertInstanceOf(AttendanceModuleException.class, error);
        assertEquals(HttpStatus.CONFLICT, ((AttendanceModuleException) error).status());
    }

    private void openAttendance() { publish(day); attendance.checkIn(employee); }
    private ScheduleResponse draft(LocalDate date) {
        return schedules.create(supervisor, new WriteRequest(teamId, date, date, List.of(
                new EntryRequest(null, employeeId, date, LocalTime.of(9, 0), LocalTime.of(17, 0), "AS08 isolated fixture"))));
    }
    private void publish(LocalDate date) { schedules.publish(supervisor, draft(date).id()); }
    private int attendanceCount(LocalDate date) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM attendance_record WHERE employee_id=? AND attendance_date=?", Integer.class, employeeId, date);
    }
    private int publishedCount(LocalDate date) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM schedule_entry e JOIN schedule s ON s.schedule_id=e.schedule_id WHERE e.employee_id=? AND e.work_date=? AND s.status='PUBLISHED'", Integer.class, employeeId, date);
    }
    private void assertStableCompletion(String checkout, String hours) {
        assertEquals(1, attendanceCount(day));
        var row = jdbc.queryForMap("SELECT CAST(check_out_time AS CHAR) AS checkout, working_hours FROM attendance_record WHERE employee_id=? AND attendance_date=?", employeeId, day);
        assertEquals(checkout, row.get("checkout"));
        assertEquals(new BigDecimal(hours), row.get("working_hours"));
    }
    private void assertBalance(String available, String used) {
        var row = jdbc.queryForMap("SELECT available_days,used_days FROM leave_balance WHERE employee_id=? AND leave_type_id=?", employeeId, leaveTypeId);
        assertEquals(new BigDecimal(available), row.get("available_days"));
        assertEquals(new BigDecimal(used), row.get("used_days"));
    }
    private String requestStatus(Long id) { return jdbc.queryForObject("SELECT status FROM leave_request WHERE leave_request_id=?", String.class, id); }
    private Long insertEmployee(String email, Long reportingTo) {
        jdbc.update("INSERT INTO employee(department_id,team_id,supervisor_id,first_name,last_name,email,hire_date,job_title,status) VALUES (?,?,?,'AS08','Fixture',?,'2026-01-01','Verification','ACTIVE')", departmentId, teamId, reportingTo, email);
        return jdbc.queryForObject("SELECT employee_id FROM employee WHERE email=?", Long.class, email);
    }
    private AccountPrincipal principal(Long id, String role) {
        AccountPrincipal value = mock(AccountPrincipal.class);
        when(value.getEmployeeId()).thenReturn(id);
        when(value.getRole()).thenReturn(role);
        return value;
    }
}
