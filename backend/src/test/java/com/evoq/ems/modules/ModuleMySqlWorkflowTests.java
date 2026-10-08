package com.evoq.ems.modules;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import com.evoq.ems.auth.*;
import com.evoq.ems.asset.domain.AssetAssignment;
import com.evoq.ems.asset.repository.AssetAssignmentRepository;
import com.evoq.ems.asset.service.*;
import com.evoq.ems.asset.controller.AssetReportController;
import com.evoq.ems.employee.service.*;
import com.evoq.ems.employee.web.EmployeeDtos.*;
import com.evoq.ems.employee.web.EmployeeReportController;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.leave.web.LeaveDtos.*;
import com.evoq.ems.leave.web.LeaveReportController;
import com.evoq.ems.attendance.service.AttendanceReconciliationJob;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** Isolated committed fixtures enable real competing transactions; cleanup uses only fixture IDs. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ModuleMySqlWorkflowTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean EmployeeTeamReader people;
    @Autowired EmployeeService employees;
    @Autowired EmployeeAccess access;
    @Autowired AssetAssignmentService assignments;
    @Autowired AssetService assets;
    @Autowired LeaveService leave;
    @Autowired EmployeeReportController employeeReports;
    @Autowired LeaveReportController leaveReports;
    @Autowired AssetReportController assetReports;
    @Autowired UserAccountRepository accounts;
    @Autowired DevDemoAccountInitializer demoInitializer;
    @Autowired RoleRepository roles;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @MockitoSpyBean AssetAssignmentRepository assignmentRepository;
    @MockitoBean AttendanceReconciliationJob job;
    Long departmentId, teamId, otherTeamId, supervisorId, employeeId, otherEmployeeId, assetId, typeId;
    String tag;
    ExecutorService workers;

    @BeforeEach void fixtures() {
        tag = "Gaps-" + UUID.randomUUID();
        jdbc.update("INSERT INTO department(name) VALUES (?)", tag);
        departmentId = jdbc.queryForObject("SELECT department_id FROM department WHERE name=?", Long.class, tag);
        jdbc.update("INSERT INTO team_project(name) VALUES (?)", tag);
        teamId = jdbc.queryForObject("SELECT team_id FROM team_project WHERE name=?", Long.class, tag);
        jdbc.update("INSERT INTO team_project(name) VALUES (?)", tag + "-other-team");
        otherTeamId = jdbc.queryForObject("SELECT team_id FROM team_project WHERE name=?", Long.class, tag + "-other-team");
        supervisorId = insertEmployee("supervisor", null);
        employeeId = insertEmployee("employee", supervisorId);
        otherEmployeeId = insertEmployee("other", null);
        jdbc.update("INSERT INTO asset(asset_name,asset_type,serial_number,status) VALUES (?,?,?,'AVAILABLE')", tag, "Laptop", tag);
        assetId = jdbc.queryForObject("SELECT asset_id FROM asset WHERE serial_number=?", Long.class, tag);
        jdbc.update("INSERT INTO leave_type(name) VALUES (?)", tag);
        typeId = jdbc.queryForObject("SELECT leave_type_id FROM leave_type WHERE name=?", Long.class, tag);
    }
    Long insertEmployee(String name, Long supervisor) {
        String email = tag + "-" + name + "@example.invalid";
        jdbc.update("INSERT INTO employee(department_id,team_id,supervisor_id,first_name,last_name,email,hire_date,job_title,status) VALUES (?,?,?,?,'Fixture',?,'2026-01-01','Engineer','ACTIVE')", departmentId, teamId, supervisor, name, email);
        return jdbc.queryForObject("SELECT employee_id FROM employee WHERE email=?", Long.class, email);
    }
    @AfterEach void cleanup() throws Exception {
        if (workers != null) { workers.shutdownNow(); assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS)); }
        if (departmentId == null) return;
        jdbc.update("DELETE FROM asset_assignment WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
        if (assetId != null) jdbc.update("DELETE FROM asset WHERE asset_id=?", assetId);
        jdbc.update("DELETE FROM leave_request WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
        jdbc.update("DELETE FROM leave_balance WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
        jdbc.update("DELETE FROM user_account WHERE employee_id IN (SELECT employee_id FROM employee WHERE department_id=?)", departmentId);
        jdbc.update("DELETE FROM employee WHERE department_id=?", departmentId);
        if (typeId != null) jdbc.update("DELETE FROM leave_type WHERE leave_type_id=?", typeId);
        if (teamId != null) jdbc.update("DELETE FROM team_project WHERE team_id=?", teamId);
        if (otherTeamId != null) jdbc.update("DELETE FROM team_project WHERE team_id=?", otherTeamId);
        jdbc.update("DELETE FROM department WHERE department_id=?", departmentId);
    }
    AccountPrincipal caller(Long id, String role) { var p = mock(AccountPrincipal.class); when(p.getEmployeeId()).thenReturn(id); when(p.getRole()).thenReturn(role); return p; }
    String assetStatus() { return jdbc.queryForObject("SELECT status FROM asset WHERE asset_id=?", String.class, assetId); }
    int activeAssignments() { return jdbc.queryForObject("SELECT COUNT(*) FROM asset_assignment WHERE asset_id=? AND status='ASSIGNED'", Integer.class, assetId); }

    @Test void assignmentWriteFailureRollsBackAssetStatus() {
        doThrow(new IllegalStateException("Injected persistence failure")).when(assignmentRepository).save(argThat(a -> a != null && assetId.equals(a.getAssetId())));
        assertThrows(IllegalStateException.class, () -> assignments.assignAsset(assetId, employeeId));
        assertEquals("AVAILABLE", assetStatus()); assertEquals(0, activeAssignments());
    }
    @Test void invalidEmployeeLeavesAssetAvailableAndReturnRetainsHistory() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> assignments.assignAsset(assetId, -1L)).getStatusCode().value());
        assertEquals("AVAILABLE", assetStatus()); assertEquals(0, activeAssignments());
        var assignment = assignments.assignAsset(assetId, employeeId);
        assertEquals("ASSIGNED", assetStatus());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assets.updateStatus(assetId, "AVAILABLE")).getStatusCode().value());
        assignments.returnAsset(assignment.getAssignmentId());
        assertEquals("AVAILABLE", assetStatus()); assertEquals(0, activeAssignments());
        var history = assignments.getAssignmentsByEmployee(employeeId);
        assertEquals(1, history.size()); assertEquals("RETURNED", history.getFirst().assignmentStatus()); assertNotNull(history.getFirst().returnedDate());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.returnAsset(assignment.getAssignmentId())).getStatusCode().value());
    }
    @Test void competingAssignmentsProduceExactlyOneLinkAndOneConflict() throws Exception {
        workers = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Integer> one = () -> { start.await(); try { assignments.assignAsset(assetId, employeeId); return 200; } catch (ResponseStatusException e) { return e.getStatusCode().value(); } };
        Callable<Integer> two = () -> { start.await(); try { assignments.assignAsset(assetId, otherEmployeeId); return 200; } catch (ResponseStatusException e) { return e.getStatusCode().value(); } };
        var first = workers.submit(one); var second = workers.submit(two); start.countDown();
        var outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)).stream().sorted().toList();
        assertEquals(List.of(200, 409), outcomes); assertEquals("ASSIGNED", assetStatus()); assertEquals(1, activeAssignments());
    }
    @Test void competingReturnsDoNotOverwriteReassignedAsset() throws Exception {
        var assignment = assignments.assignAsset(assetId, employeeId);
        workers = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Integer> action = () -> { start.await(); try { assignments.returnAsset(assignment.getAssignmentId()); return 200; } catch (ResponseStatusException e) { return e.getStatusCode().value(); } };
        var first = workers.submit(action); var second = workers.submit(action); start.countDown();
        assertEquals(List.of(200, 409), List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)).stream().sorted().toList());
        assertEquals("AVAILABLE", assetStatus()); assertEquals(0, activeAssignments());
        assignments.assignAsset(assetId, otherEmployeeId);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.returnAsset(assignment.getAssignmentId())).getStatusCode().value());
        assertEquals("ASSIGNED", assetStatus()); assertEquals(1, activeAssignments());
    }
    @Test void scopedEmployeeLeaveAndAssetReportsContainOnlyPermittedFacts() {
        var self = caller(employeeId, "EMPLOYEE"); var supervisor = caller(supervisorId, "SUPERVISOR");
        assertEquals(List.of(employeeId), access.visibleIds(self));
        assertEquals(2, access.visibleIds(supervisor).size());
        assertThrows(AccessDeniedException.class, () -> employees.getEmployeeById(self, otherEmployeeId));
        assertEquals(1, employees.getDirectReports(caller(supervisorId, "MANAGER_ADMIN"), supervisorId).size());
        assertThrows(RuntimeException.class, () -> employees.getDirectReports(supervisor, otherEmployeeId));
        assertEquals(1, employeeReports.report(self, null, null, null).summary().get("Employees in scope"));
        assertEquals(2, employeeReports.report(supervisor, null, null, null).summary().get("Employees in scope"));
        leave.setEntitlement(employeeId, typeId, new EntitlementRequest(new BigDecimal("12")));
        leave.setEntitlement(otherEmployeeId, typeId, new EntitlementRequest(new BigDecimal("30")));
        var request = leave.submit(employeeId, new SubmitLeaveRequest(typeId, LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 2), "Fixture"));
        var report = leaveReports.report(self, null, null, null);
        assertEquals(1, report.summary().get("Leave requests")); assertEquals(new BigDecimal("12.00"), report.summary().get("Available days (current balances)"));
        assertThrows(AccessDeniedException.class, () -> leaveReports.report(supervisor, null, null, otherEmployeeId));
        leave.approve(request.id(), supervisor);
        assertEquals(new BigDecimal("2.00"), leave.getMyLeave(employeeId).balances().getFirst().usedDays());
        leave.setEntitlement(employeeId, typeId, new EntitlementRequest(new BigDecimal("20")));
        assertEquals(new BigDecimal("18.00"), leave.getMyLeave(employeeId).balances().getFirst().availableDays());
        assignments.assignAsset(assetId, otherEmployeeId);
        assertTrue(assets.getAllAssets(self).isEmpty());
        assertEquals(0, assetReports.report(self, null).summary().get("Currently assigned assets"));
        assertThrows(AccessDeniedException.class, () -> assetReports.report(self, otherEmployeeId));
        assertEquals(1, assetReports.report(caller(supervisorId, "MANAGER_ADMIN"), otherEmployeeId).tables().getFirst().rows().size());
    }
    @Test void deactivationRetainsHistoryAndDoesNotInventMandatoryAssetClearance() {
        assignments.assignAsset(assetId, employeeId);
        employees.changeEmployeeStatus(employeeId, EmployeeStatus.INACTIVE);
        assertEquals("INACTIVE", jdbc.queryForObject("SELECT status FROM employee WHERE employee_id=?", String.class, employeeId));
        assertEquals(1, activeAssignments());
        assertEquals(1, assignments.getAssignmentsByEmployee(employeeId).size());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> assignments.assignAsset(assetId, employeeId)).getStatusCode().value());
    }

    @Test void inactiveOnboardingDisablesLoginAndSupervisorSelectionRejectsOrdinaryAccount() {
        var created = employees.createEmployee(new CreateEmployeeRequest(departmentId, teamId, null, "Inactive", "Fixture",
                tag + "-inactive@example.invalid", null, null, LocalDate.now(), "Engineer", EmployeeStatus.INACTIVE,
                true, tag.toLowerCase(), "FixturePassword123!", "EMPLOYEE"));
        assertFalse(accounts.findByEmployeeId(created.id()).orElseThrow().isActive());
        var principal = new AccountPrincipal(accounts.findByEmployeeId(created.id()).orElseThrow()); assertFalse(principal.isEnabled());
        assertFalse(employees.getSupervisorCandidates().stream().anyMatch(e -> e.id().equals(created.id())));
        assertThrows(RuntimeException.class, () -> employees.createEmployee(new CreateEmployeeRequest(departmentId, teamId, otherEmployeeId,
                "Invalid", "Supervisor", tag + "-invalid@example.invalid", null, null, LocalDate.now(), "Engineer", EmployeeStatus.ACTIVE,
                false, null, null, null)));
    }
    @Test void returnedAssetKeepsItsNameAfterReturnAndReassignment() {
        var assignment = assignments.assignAsset(assetId, employeeId);
        assignments.returnAsset(assignment.getAssignmentId());
        assertTrue(assets.getAllAssets(caller(employeeId, "EMPLOYEE")).isEmpty());
        assertEquals(tag, assignments.getAssignmentsByEmployee(employeeId).getFirst().assetName());
        assignments.assignAsset(assetId, otherEmployeeId);
        assertTrue(assets.getAllAssets(caller(employeeId, "EMPLOYEE")).isEmpty());
        var history = assignments.getAssignmentsByEmployee(employeeId);
        assertEquals(1, history.size()); assertEquals(tag, history.getFirst().assetName());
        assertEquals("RETURNED", history.getFirst().assignmentStatus()); assertEquals(employeeId, history.getFirst().employeeId());
        assertEquals(2, assignments.getAssignmentHistory(assetId).size());
    }

    Long pendingLeave() {
        leave.setEntitlement(employeeId, typeId, new EntitlementRequest(new BigDecimal("12")));
        return leave.submit(employeeId, new SubmitLeaveRequest(typeId, LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 2), "Scope fixture")).id();
    }
    void changeScope(String change) {
        switch (change) {
            case "transfer" -> jdbc.update("UPDATE employee SET team_id=? WHERE employee_id=?", otherTeamId, employeeId);
            case "inactive" -> employees.changeEmployeeStatus(employeeId, EmployeeStatus.INACTIVE);
            case "no-team" -> jdbc.update("UPDATE employee SET team_id=NULL WHERE employee_id=?", employeeId);
            case "reassigned" -> jdbc.update("UPDATE employee SET supervisor_id=? WHERE employee_id=?", otherEmployeeId, employeeId);
            case "supervisor-transfer" -> jdbc.update("UPDATE employee SET team_id=? WHERE employee_id=?", otherTeamId, supervisorId);
            case "supervisor-no-team" -> jdbc.update("UPDATE employee SET team_id=NULL WHERE employee_id=?", supervisorId);
            case "supervisor-inactive" -> employees.changeEmployeeStatus(supervisorId, EmployeeStatus.INACTIVE);
            default -> throw new IllegalArgumentException(change);
        }
    }
    void assertUntouchedPending(Long id) {
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM leave_request WHERE leave_request_id=?", String.class, id));
        assertEquals(new BigDecimal("12.00"), leave.getMyLeave(employeeId).balances().getFirst().availableDays());
        assertEquals(new BigDecimal("0.00"), leave.getMyLeave(employeeId).balances().getFirst().usedDays());
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"SUPERVISOR,transfer", "SUPERVISOR,inactive", "SUPERVISOR,no-team", "SUPERVISOR,reassigned", "SUPERVISOR,supervisor-transfer", "SUPERVISOR,supervisor-no-team", "SUPERVISOR,supervisor-inactive",
            "MANAGER_ADMIN,transfer", "MANAGER_ADMIN,inactive", "MANAGER_ADMIN,no-team", "MANAGER_ADMIN,reassigned", "MANAGER_ADMIN,supervisor-transfer", "MANAGER_ADMIN,supervisor-no-team", "MANAGER_ADMIN,supervisor-inactive"})
    void pendingQueueAndDecisionsUseTheSameCurrentActiveTeamBoundary(String role, String change) {
        Long id = pendingLeave(); var supervisor = caller(supervisorId, role);
        assertEquals(List.of(id), leave.getPendingForApprover(supervisor).stream().map(RequestResponse::id).toList());
        changeScope(change);
        assertTrue(leave.getPendingForApprover(supervisor).isEmpty());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> leave.approve(id, supervisor)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> leave.reject(id, supervisor)).getStatusCode().value());
        assertUntouchedPending(id);
        assertTrue(leave.getAllRequests().stream().anyMatch(r -> r.id().equals(id)));
    }
    @Test void newAssignedSameTeamSupervisorCanRejectAfterTransfer() {
        Long id = pendingLeave();
        jdbc.update("UPDATE employee SET team_id=?,supervisor_id=? WHERE employee_id=?", otherTeamId, otherEmployeeId, employeeId);
        jdbc.update("UPDATE employee SET team_id=? WHERE employee_id=?", otherTeamId, otherEmployeeId);
        assertTrue(leave.getPendingForApprover(caller(supervisorId, "SUPERVISOR")).isEmpty());
        assertEquals(List.of(id), leave.getPendingForApprover(caller(otherEmployeeId, "SUPERVISOR")).stream().map(RequestResponse::id).toList());
        assertEquals("REJECTED", leave.reject(id, caller(otherEmployeeId, "SUPERVISOR")).status());
        assertTrue(leave.getPendingForApprover(caller(otherEmployeeId, "SUPERVISOR")).isEmpty());
        assertEquals(new BigDecimal("0.00"), leave.getMyLeave(employeeId).balances().getFirst().usedDays());
    }
    @Test void decisionWaitingForEmployeeEditRechecksCommittedTransfer() throws Exception {
        Long id = pendingLeave(); workers = Executors.newFixedThreadPool(2);
        var edited = new CountDownLatch(1); var deciding = new CountDownLatch(1);
        doAnswer(invocation -> { if (employeeId.equals(invocation.getArgument(0))) deciding.countDown(); return invocation.callRealMethod(); })
                .when(people).lockEmployee(anyLong());
        var edit = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("UPDATE employee SET team_id=? WHERE employee_id=?", otherTeamId, employeeId);
            edited.countDown();
            try { assertTrue(deciding.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        }));
        assertTrue(edited.await(10, TimeUnit.SECONDS));
        var decision = workers.submit(() -> {
            try { leave.approve(id, caller(supervisorId, "SUPERVISOR")); return 200; }
            catch (ResponseStatusException e) { return e.getStatusCode().value(); }
        });
        edit.get(15, TimeUnit.SECONDS); assertEquals(403, decision.get(15, TimeUnit.SECONDS)); assertUntouchedPending(id);
    }
    @ParameterizedTest
    @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR", "MANAGER_ADMIN"})
    void inactiveCreationAndOfficialEditorStatusControlRealLoginAndExistingSessions(String role) throws Exception {
        String username = tag.toLowerCase(); String password = "FixturePassword123!";
        var created = employees.createEmployee(new CreateEmployeeRequest(departmentId, teamId, null, "Status", "Fixture",
                tag + "-login@example.invalid", null, null, LocalDate.now(), "Engineer", EmployeeStatus.INACTIVE,
                true, username, password, role));
        login(username, password, 401);
        employees.updateOfficialInfo(created.id(), new UpdateOfficialInfoRequest(departmentId, teamId, null, "Engineer", EmployeeStatus.ACTIVE, null));
        var session = login(username, password, 200);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk());
        employees.updateOfficialInfo(created.id(), new UpdateOfficialInfoRequest(departmentId, teamId, null, "Engineer", EmployeeStatus.INACTIVE, null));
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid()); login(username, password, 401);
        assertEquals(EmployeeStatus.INACTIVE, employees.getEmployeeById(created.id()).status());
        assertFalse(accounts.findByEmployeeId(created.id()).orElseThrow().isActive());
    }
    MockHttpSession login(String username, String password, int expected) throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username", username).param("password", password))
                .andExpect(status().is(expected)).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    void enableManagerFixture() {
        accounts.save(new UserAccount(supervisorId, roles.findByName("MANAGER_ADMIN").orElseThrow(), tag.toLowerCase() + ".manager", encoder.encode("FixturePassword123!"), true));
    }
    @Test void managerWithoutTeamOrSupervisorCanApproveOwnLeaveThroughHttpOnce() throws Exception {
        enableManagerFixture();
        jdbc.update("UPDATE employee SET team_id=NULL,supervisor_id=NULL WHERE employee_id=?", supervisorId);
        leave.setEntitlement(supervisorId, typeId, new EntitlementRequest(new BigDecimal("12")));
        Long id = leave.submit(supervisorId, new SubmitLeaveRequest(typeId, LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 2), "Manager own")).id();
        var session = login(tag.toLowerCase() + ".manager", "FixturePassword123!", 200);
        mvc.perform(get("/api/leave/pending?employeeId=" + employeeId).session(session)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].id").value(id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(1));
        mvc.perform(post("/api/leave/requests/" + id + "/reject").session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/leave/requests/" + id + "/approve").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/leave/requests/" + id + "/approve").session(session).with(csrf())).andExpect(status().isConflict());
        assertEquals(new BigDecimal("10.00"), leave.getMyLeave(supervisorId).balances().getFirst().availableDays());
        assertEquals(new BigDecimal("2.00"), leave.getMyLeave(supervisorId).balances().getFirst().usedDays());
        assertTrue(leave.getPendingForApprover(caller(supervisorId, "MANAGER_ADMIN")).isEmpty());
        assertEquals(1, accounts.findByEmployeeId(supervisorId).stream().count());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"approve", "reject"})
    void assignedManagerDecidesSupervisorLeaveButCannotDecideUnrelatedLeave(String decision) throws Exception {
        enableManagerFixture();
        // Supervisor onboarding uses the already supported Manager supervisor candidate.
        var report = employees.createEmployee(new CreateEmployeeRequest(departmentId, teamId, supervisorId, "Supervisor", "Report",
                tag + "-supervisor-report@example.invalid", null, null, LocalDate.now(), "Supervisor", EmployeeStatus.ACTIVE,
                true, tag.toLowerCase() + ".report", "FixturePassword123!", "SUPERVISOR"));
        leave.setEntitlement(report.id(), typeId, new EntitlementRequest(new BigDecimal("12")));
        Long id = leave.submit(report.id(), new SubmitLeaveRequest(typeId, LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 2), "Supervisor request")).id();
        leave.setEntitlement(otherEmployeeId, typeId, new EntitlementRequest(new BigDecimal("12")));
        Long unrelated = leave.submit(otherEmployeeId, new SubmitLeaveRequest(typeId, LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 2), "Unrelated")).id();
        var session = login(tag.toLowerCase() + ".manager", "FixturePassword123!", 200);
        assertEquals(List.of(id), leave.getPendingForApprover(caller(supervisorId, "MANAGER_ADMIN")).stream().map(RequestResponse::id).toList());
        mvc.perform(post("/api/leave/requests/" + unrelated + "/" + decision).session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/leave/requests/" + id + "/" + decision).session(session).with(csrf())).andExpect(status().isOk());
        assertEquals(decision.equals("approve") ? "APPROVED" : "REJECTED", leave.getMyLeave(report.id()).requests().getFirst().status());
        assertEquals(new BigDecimal(decision.equals("approve") ? "2.00" : "0.00"), leave.getMyLeave(report.id()).balances().getFirst().usedDays());
        assertEquals("PENDING", leave.getMyLeave(otherEmployeeId).requests().getFirst().status());
    }

    @Test void mergedDevSeederDoesNotDeleteAnAccountOwnedByAnotherEmployee() {
        Long accountId = jdbc.queryForObject("SELECT user_id FROM user_account WHERE username='a.perera'", Long.class);
        Long ownerId = jdbc.queryForObject("SELECT employee_id FROM user_account WHERE user_id=?", Long.class, accountId);
        String hash = jdbc.queryForObject("SELECT password_hash FROM user_account WHERE user_id=?", String.class, accountId);
        jdbc.update("UPDATE user_account SET employee_id=? WHERE user_id=?", employeeId, accountId);
        try {
            assertThrows(IllegalStateException.class, () -> demoInitializer.run(null));
            assertEquals(accountId, jdbc.queryForObject("SELECT user_id FROM user_account WHERE username='a.perera'", Long.class));
            assertEquals(employeeId, jdbc.queryForObject("SELECT employee_id FROM user_account WHERE user_id=?", Long.class, accountId));
            assertEquals(hash, jdbc.queryForObject("SELECT password_hash FROM user_account WHERE user_id=?", String.class, accountId));
        } finally {
            jdbc.update("UPDATE user_account SET employee_id=? WHERE user_id=?", ownerId, accountId);
        }
    }
}
