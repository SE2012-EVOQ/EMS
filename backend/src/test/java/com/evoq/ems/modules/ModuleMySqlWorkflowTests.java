package com.evoq.ems.modules;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
@ActiveProfiles("dev")
class ModuleMySqlWorkflowTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired EmployeeService employees;
    @Autowired EmployeeAccess access;
    @Autowired AssetAssignmentService assignments;
    @Autowired AssetService assets;
    @Autowired LeaveService leave;
    @Autowired EmployeeReportController employeeReports;
    @Autowired LeaveReportController leaveReports;
    @Autowired AssetReportController assetReports;
    @Autowired UserAccountRepository accounts;
    @MockitoSpyBean AssetAssignmentRepository assignmentRepository;
    @MockitoBean AttendanceReconciliationJob job;
    Long departmentId, teamId, supervisorId, employeeId, otherEmployeeId, assetId, typeId;
    String tag;
    ExecutorService workers;

    @BeforeEach void fixtures() {
        tag = "Gaps-" + UUID.randomUUID();
        jdbc.update("INSERT INTO department(name) VALUES (?)", tag);
        departmentId = jdbc.queryForObject("SELECT department_id FROM department WHERE name=?", Long.class, tag);
        jdbc.update("INSERT INTO team_project(name) VALUES (?)", tag);
        teamId = jdbc.queryForObject("SELECT team_id FROM team_project WHERE name=?", Long.class, tag);
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
        assertEquals(1, history.size()); assertEquals("RETURNED", history.getFirst().getAssignmentStatus()); assertNotNull(history.getFirst().getReturnedDate());
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
}
