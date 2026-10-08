package com.evoq.ems.leave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.domain.*;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.service.ApprovedLeaveScheduleCoordinator;
import com.evoq.ems.leave.domain.*;
import com.evoq.ems.leave.repository.*;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.leave.web.LeaveDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LeaveServiceTests {
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final LeaveTypeRepository types = mock(LeaveTypeRepository.class);
    final LeaveBalanceRepository balances = mock(LeaveBalanceRepository.class);
    final LeaveRequestRepository requests = mock(LeaveRequestRepository.class);
    final ApprovedLeaveScheduleCoordinator schedules = mock(ApprovedLeaveScheduleCoordinator.class);
    final EmployeeTeamReader people = mock(EmployeeTeamReader.class);
    final LeaveService service = new LeaveService(employees, types, balances, requests, schedules, people);
    final Employee employee = new Employee(null, null, null, "A", "B", "a@b.com", null, null, LocalDate.now(), "Engineer", EmployeeStatus.ACTIVE);
    final LeaveType type = new LeaveType("Annual", null);
    @Test void readsDoNotCreateTypesOrBalances() {
        assertTrue(service.getTypes().isEmpty()); assertTrue(service.getMyLeave(1L).balances().isEmpty());
        verify(types, never()).save(any()); verify(balances, never()).save(any()); verifyNoInteractions(employees);
    }
    @Test void entitlementPreservesUsedDaysAndRejectsGrantBelowUsage() {
        var balance = new LeaveBalance(employee, type, new BigDecimal("14")); balance.approveDays(new BigDecimal("4"));
        when(employees.findLockedById(1L)).thenReturn(Optional.of(employee)); when(types.findById(2L)).thenReturn(Optional.of(type));
        when(balances.findLockedForEmployeeType(1L, 2L)).thenReturn(Optional.of(balance)); when(balances.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.setEntitlement(1L, 2L, new EntitlementRequest(new BigDecimal("20")));
        assertEquals(new BigDecimal("16"), response.availableDays()); assertEquals(new BigDecimal("4"), response.usedDays());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.setEntitlement(1L, 2L,
                new EntitlementRequest(new BigDecimal("3")))).getStatusCode().value());
        assertEquals(new BigDecimal("16"), balance.getAvailableDays());
    }
    @Test void submitRequiresConfiguredBalanceAndDoesNotProvisionIt() {
        when(employees.findById(1L)).thenReturn(Optional.of(employee)); when(types.findById(2L)).thenReturn(Optional.of(type));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.submit(1L,
                new SubmitLeaveRequest(2L, LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 1), null))).getStatusCode().value());
        verify(balances, never()).save(any()); verify(requests, never()).save(any());
    }
    @Test void overlappingRequestsCannotBeSubmitted() {
        when(employees.findById(1L)).thenReturn(Optional.of(employee)); when(types.findById(2L)).thenReturn(Optional.of(type));
        when(requests.countOverlappingRequests(any(), any(), any())).thenReturn(1L);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.submit(1L,
                new SubmitLeaveRequest(2L, LocalDate.now(), LocalDate.now(), null))).getStatusCode().value());
    }
    @Test void leaveReportsUseOnlyAuthorizedEmployeeIds() {
        Employee self = mock(Employee.class); when(self.getId()).thenReturn(1L); when(employees.findById(1L)).thenReturn(Optional.of(self));
        AccountPrincipal caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(1L); when(caller.getRole()).thenReturn("EMPLOYEE");
        service.reportRequests(caller); service.reportBalances(caller);
        verify(requests).findForEmployees(List.of(1L)); verify(balances).findForEmployees(List.of(1L)); verify(requests, never()).findAllForManager();
    }
    @Test void onlyAssignedSupervisorCanDecideAndBalanceIsUnchangedOnDenial() {
        Employee supervisor = mock(Employee.class); when(supervisor.getId()).thenReturn(9L); employee.setSupervisor(supervisor);
        Employee requester = spy(employee); when(requester.getId()).thenReturn(1L);
        var request = new LeaveRequest(requester, type, LocalDate.now(), LocalDate.now(), null);
        when(requests.findLockedById(3L)).thenReturn(Optional.of(request));
        AccountPrincipal other = mock(AccountPrincipal.class); when(other.getEmployeeId()).thenReturn(8L); when(other.getRole()).thenReturn("SUPERVISOR");
        when(people.employee(1L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(1L, "A B", 2L, 9L, "ACTIVE")));
        when(people.employee(8L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(8L, "Other", 2L, null, "ACTIVE")));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.approve(3L, other)).getStatusCode().value());
        assertEquals("PENDING", request.getStatus()); verifyNoInteractions(schedules, balances);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"inactive", "transferred", "no-team", "different-supervisor", "supervisor-no-team", "supervisor-inactive"})
    void currentScopeRejectsBothDecisionsBeforeAnyMutation(String change) {
        Employee requester = mock(Employee.class); when(requester.getId()).thenReturn(1L);
        var request = new LeaveRequest(requester, type, LocalDate.now(), LocalDate.now(), null);
        when(requests.findLockedById(3L)).thenReturn(Optional.of(request));
        AccountPrincipal caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(9L); when(caller.getRole()).thenReturn("SUPERVISOR");
        when(people.employee(1L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(1L, "Requester",
                change.equals("no-team") ? null : change.equals("transferred") ? 4L : 2L,
                change.equals("different-supervisor") ? 8L : 9L, change.equals("inactive") ? "INACTIVE" : "ACTIVE")));
        when(people.employee(9L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(9L, "Supervisor",
                change.equals("supervisor-no-team") ? null : 2L, null, change.equals("supervisor-inactive") ? "INACTIVE" : "ACTIVE")));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.approve(3L, caller)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.reject(3L, caller)).getStatusCode().value());
        assertEquals("PENDING", request.getStatus()); verifyNoInteractions(schedules, balances); verify(requests, never()).save(any());
        var order = inOrder(people); order.verify(people).lockEmployee(1L); order.verify(people).lockEmployee(9L);
        order.verify(people).employee(1L); order.verify(people).employee(9L);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"EMPLOYEE"})
    void employeeCannotDecideLeave(String role) {
        var caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(9L); when(caller.getRole()).thenReturn(role);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.approve(3L, caller)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.reject(3L, caller)).getStatusCode().value());
        verifyNoInteractions(requests, people, schedules, balances);
    }
    @Test void selfApprovalAndRejectionRemainForbidden() {
        var caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(1L); when(caller.getRole()).thenReturn("SUPERVISOR");
        var requester = mock(Employee.class); when(requester.getId()).thenReturn(1L);
        var request = new LeaveRequest(requester, type, LocalDate.now(), LocalDate.now(), null);
        when(requests.findLockedById(3L)).thenReturn(Optional.of(request));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.approve(3L, caller)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.reject(3L, caller)).getStatusCode().value());
        verifyNoInteractions(people, schedules, balances); verify(requests, never()).save(any());
    }

    @Test void managerSelfApprovalUsesNormalBalanceAndScheduleChecksWithoutTeamOrSupervisor() {
        var caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(1L); when(caller.getRole()).thenReturn("MANAGER_ADMIN");
        var requester = mock(Employee.class); when(requester.getId()).thenReturn(1L);
        var leaveType = mock(LeaveType.class); when(leaveType.getId()).thenReturn(2L);
        var request = new LeaveRequest(requester, leaveType, LocalDate.now(), LocalDate.now().plusDays(1), null);
        var balance = new LeaveBalance(requester, leaveType, new BigDecimal("12"));
        when(requests.findLockedById(3L)).thenReturn(Optional.of(request));
        when(people.employee(1L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(1L, "Manager", null, null, "ACTIVE")));
        when(schedules.removeFutureShifts(any(), any(), any())).thenReturn(true);
        when(balances.findLockedForEmployeeType(1L, 2L)).thenReturn(Optional.of(balance));
        when(requests.save(request)).thenReturn(request);
        assertEquals("APPROVED", service.approve(3L, caller).status());
        assertEquals(new BigDecimal("10"), balance.getAvailableDays()); assertEquals(new BigDecimal("2"), balance.getUsedDays());
        verify(people, times(1)).lockEmployee(1L);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.approve(3L, caller)).getStatusCode().value());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"inactive", "attendance", "balance", "self-reject"})
    void managerOwnExceptionDoesNotBypassOtherGuards(String condition) {
        var caller = mock(AccountPrincipal.class); when(caller.getEmployeeId()).thenReturn(1L); when(caller.getRole()).thenReturn("MANAGER_ADMIN");
        var requester = mock(Employee.class); when(requester.getId()).thenReturn(1L);
        var leaveType = mock(LeaveType.class); when(leaveType.getId()).thenReturn(2L);
        var request = new LeaveRequest(requester, leaveType, LocalDate.now(), LocalDate.now(), null);
        var balance = new LeaveBalance(requester, leaveType, BigDecimal.ZERO);
        when(requests.findLockedById(3L)).thenReturn(Optional.of(request));
        when(people.employee(1L)).thenReturn(Optional.of(new EmployeeTeamReader.EmployeeInfo(1L, "Manager", null, null, condition.equals("inactive") ? "INACTIVE" : "ACTIVE")));
        when(schedules.removeFutureShifts(any(), any(), any())).thenReturn(!condition.equals("attendance"));
        when(balances.findLockedForEmployeeType(1L, 2L)).thenReturn(Optional.of(balance));
        int expected = condition.equals("balance") ? 400 : condition.equals("attendance") ? 409 : 403;
        assertEquals(expected, assertThrows(ResponseStatusException.class, () -> {
            if (condition.equals("self-reject")) service.reject(3L, caller); else service.approve(3L, caller);
        }).getStatusCode().value());
        assertEquals("PENDING", request.getStatus()); assertEquals(BigDecimal.ZERO, balance.getUsedDays()); verify(requests, never()).save(any());
    }
}
