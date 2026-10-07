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
        AccountPrincipal other = mock(AccountPrincipal.class); when(other.getEmployeeId()).thenReturn(8L);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.approve(3L, other)).getStatusCode().value());
        assertEquals("PENDING", request.getStatus()); verifyNoInteractions(schedules, balances);
    }
}
