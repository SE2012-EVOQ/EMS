package com.evoq.ems.modules;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.Optional;
import com.evoq.ems.auth.*;
import com.evoq.ems.asset.controller.*;
import com.evoq.ems.asset.service.*;
import com.evoq.ems.employee.web.*;
import com.evoq.ems.employee.service.*;
import com.evoq.ems.employee.repository.*;
import com.evoq.ems.leave.web.*;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.leave.service.LeaveReportService;
import com.evoq.ems.config.SecurityConfig;
import com.evoq.ems.common.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {EmployeeController.class, OrganizationController.class, EmployeeReportController.class,
        LeaveController.class, LeaveReportController.class, AssetController.class, AssetAssignmentController.class, AssetReportController.class})
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class, EmployeeModuleExceptionHandler.class,
        EmployeeService.class, EmployeeReportService.class, AssetReportService.class, LeaveReportService.class, EmployeeAccess.class})
class ModuleApiSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean DatabaseUserDetailsService users;
    @MockitoBean EmployeeRepository employees;
    @MockitoBean DepartmentRepository departments;
    @MockitoBean TeamProjectRepository teams;
    @MockitoBean UserAccountRepository accounts;
    @MockitoBean RoleRepository roles;
    @MockitoBean OrganizationService organization;
    @MockitoBean LeaveService leave;
    @MockitoBean AssetService assets;
    @MockitoBean AssetAssignmentService assignments;

    AccountPrincipal principal(String role) {
        Role r = mock(Role.class); when(r.getName()).thenReturn(role);
        AccountPrincipal p = new AccountPrincipal(new UserAccount(1L, r, "test." + role, "hash", true));
        when(users.loadUserByUsername(p.getUsername())).thenReturn(p); return p;
    }
    @Test void anonymousCannotReadProtectedModulesOrReports() throws Exception {
        for (String path : List.of("/api/employees", "/api/assets", "/api/leave/me", "/api/employee-reports", "/api/leave-reports", "/api/asset-reports"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR"})
    void onlyManagerCanAdministerAssetsAndLeaveSetup(String role) throws Exception {
        var p = principal(role);
        mvc.perform(post("/api/assets").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"assetName\":\"Laptop\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/assets/2").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/assets/2/status?status=AVAILABLE").with(user(p)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/asset-assignments/assign?assetId=2&employeeId=9").with(user(p)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/asset-assignments/3/return").with(user(p)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/asset-assignments").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(get("/api/asset-assignments/asset/2").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(post("/api/leave/types").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Annual\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/leave/setup/employees/9/types/2").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"entitlementDays\":14}")).andExpect(status().isForbidden());
        verifyNoInteractions(assets, assignments);
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR"})
    void assetEmployeeFilterCannotBeUsedToReadOthers(String role) throws Exception {
        var p = principal(role);
        mvc.perform(get("/api/asset-assignments/employee/9").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(get("/api/asset-assignments/employee/1").with(user(p))).andExpect(status().isOk());
        mvc.perform(get("/api/asset-reports?employeeId=9").with(user(p))).andExpect(status().isForbidden());
        verify(assignments, never()).getAssignmentsByEmployee(9L);
    }
    @Test void supervisorCannotSupplyAnotherSupervisorId() throws Exception {
        mvc.perform(get("/api/employees/9/direct-reports").with(user(principal("SUPERVISOR")))).andExpect(status().isForbidden());
        verify(employees, never()).findBySupervisorId(9L);
    }
    @Test void employeeCannotReadOthersOrEditOfficialFields() throws Exception {
        var p = principal("EMPLOYEE");
        var self = mock(com.evoq.ems.employee.domain.Employee.class); when(self.getId()).thenReturn(1L);
        when(employees.findById(1L)).thenReturn(Optional.of(self));
        mvc.perform(get("/api/employees/9").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(put("/api/employees/9/official").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"departmentId\":1,\"jobTitle\":\"Engineer\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/employees/9/contact").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"phone\":null,\"address\":null}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/employee-reports").with(user(p))).andExpect(status().isOk()).andExpect(jsonPath("$.summary['Employees in scope']").value(1));
        verify(employees, never()).findAll(); verify(employees, never()).findById(9L);
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR", "MANAGER_ADMIN"})
    void everyRoleCanSubmitOwnLeaveIgnoringClientEmployeeId(String role) throws Exception {
        var p = principal(role);
        mvc.perform(post("/api/leave/requests").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":99,\"leaveTypeId\":2,\"startDate\":\"2099-01-01\",\"endDate\":\"2099-01-02\"}"))
                .andExpect(status().isCreated());
        verify(leave).submit(eq(1L), any());
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR"})
    void leaveReportCannotOverrideEmployeeScopeEvenWithNoStoredFacts(String role) throws Exception {
        var p = principal(role);
        var self = mock(com.evoq.ems.employee.domain.Employee.class); when(self.getId()).thenReturn(1L);
        when(employees.findById(1L)).thenReturn(Optional.of(self));
        mvc.perform(get("/api/leave-reports?employeeId=9").with(user(p))).andExpect(status().isForbidden());
        verify(leave, never()).reportBalances(any()); verify(leave, never()).reportRequests(any());
        mvc.perform(get("/api/leave-reports?employeeId=1").with(user(p))).andExpect(status().isOk())
                .andExpect(jsonPath("$.summary['Leave requests']").value(0));
    }

    @Test void managerCanAdministerAssetsAndSetupAndInvalidEntitlementIsRejected() throws Exception {
        var p = principal("MANAGER_ADMIN");
        mvc.perform(post("/api/asset-assignments/assign?assetId=2&employeeId=9").with(user(p)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/asset-assignments/employee/9").with(user(p))).andExpect(status().isOk());
        mvc.perform(post("/api/leave/types").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Annual\"}")).andExpect(status().isOk());
        for (String value : List.of("-1", "1000", "1.123")) mvc.perform(put("/api/leave/setup/employees/9/types/2").with(user(p)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"entitlementDays\":" + value + "}")).andExpect(status().isBadRequest());
        verify(leave, never()).setEntitlement(any(), any(), any());
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE"})
    void employeeCannotAccessLeaveDecisionEndpoints(String role) throws Exception {
        var p = principal(role);
        mvc.perform(get("/api/leave/supervisor/pending").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(post("/api/leave/requests/3/approve").with(user(p)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/leave/requests/3/reject").with(user(p)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(leave);
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "SUPERVISOR"})
    void ownHistoryIncludesLabelWithoutExposingAnotherEmployeesAssignments(String role) throws Exception {
        var p = principal(role);
        when(assignments.getAssignmentsByEmployee(1L)).thenReturn(List.of(new AssignmentResponse(3L, 2L, "Returned laptop",
                1L, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 2), "RETURNED")));
        mvc.perform(get("/api/asset-assignments/employee/1").with(user(p))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetName").value("Returned laptop"))
                .andExpect(jsonPath("$[0].assignmentStatus").value("RETURNED"))
                .andExpect(jsonPath("$[0].serialNumber").doesNotExist());
        mvc.perform(get("/api/asset-assignments/employee/9").with(user(p))).andExpect(status().isForbidden());
        mvc.perform(get("/api/asset-assignments/asset/2").with(user(p))).andExpect(status().isForbidden());
        verify(assignments, never()).getAssignmentsByEmployee(9L); verify(assignments, never()).getAssignmentHistory(2L);
    }

    @ParameterizedTest @ValueSource(strings = {"SUPERVISOR", "MANAGER_ADMIN"})
    void approverEndpointsUseAuthenticatedIdentityAndSupportTheExistingQueueAlias(String role) throws Exception {
        var p = principal(role);
        for (String path : List.of("/api/leave/pending", "/api/leave/supervisor/pending")) {
            mvc.perform(get(path + "?employeeId=99").with(user(p))).andExpect(status().isOk());
        }
        verify(leave, times(2)).getPendingForApprover(p);
        mvc.perform(post("/api/leave/requests/3/approve").with(user(p)).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/leave/requests/3/reject").with(user(p)).with(csrf())).andExpect(status().isOk());
        verify(leave).approve(3L, p); verify(leave).reject(3L, p);
        mvc.perform(post("/api/leave/requests/3/approve").with(user(p))).andExpect(status().isForbidden());
        verify(leave, times(1)).approve(3L, p);
    }
}
