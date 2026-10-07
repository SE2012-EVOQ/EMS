package com.evoq.ems.employee;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.domain.*;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.employee.service.EmployeeAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EmployeeAccessTests {
    final EmployeeRepository repository = mock(EmployeeRepository.class);
    final EmployeeAccess access = new EmployeeAccess(repository);
    Employee person(long id, Long supervisorId, Long teamId, EmployeeStatus status) {
        Employee e = mock(Employee.class); when(e.getId()).thenReturn(id); when(e.getStatus()).thenReturn(status);
        if (supervisorId != null) { Employee supervisor = mock(Employee.class); when(supervisor.getId()).thenReturn(supervisorId); when(e.getSupervisor()).thenReturn(supervisor); }
        if (teamId != null) { TeamProject team = mock(TeamProject.class); when(team.getId()).thenReturn(teamId); when(e.getTeam()).thenReturn(team); }
        return e;
    }
    AccountPrincipal caller(String role) { var p = mock(AccountPrincipal.class); when(p.getEmployeeId()).thenReturn(1L); when(p.getRole()).thenReturn(role); return p; }
    @Test void employeeReadsOnlySelfAndCannotOverrideScope() {
        var p = caller("EMPLOYEE"); doReturn(Optional.of(person(1L, null, 3L, EmployeeStatus.ACTIVE))).when(repository).findById(1L);
        assertEquals(List.of(1L), access.visibleIds(p));
        assertThrows(AccessDeniedException.class, () -> access.requireVisible(p, 2L));
        verify(repository, never()).findAll();
    }
    @Test void supervisorReadsSelfAndOnlyActiveDirectReportsInCurrentTeam() {
        var p = caller("SUPERVISOR"); doReturn(Optional.of(person(1L, null, 3L, EmployeeStatus.ACTIVE))).when(repository).findById(1L);
        doReturn(List.of(person(2L, 1L, 3L, EmployeeStatus.ACTIVE),
                person(3L, 1L, 4L, EmployeeStatus.ACTIVE), person(4L, 1L, 3L, EmployeeStatus.INACTIVE))).when(repository).findBySupervisorId(1L);
        assertEquals(List.of(1L, 2L), access.visibleIds(p));
        assertThrows(AccessDeniedException.class, () -> access.requireVisible(p, 3L));
        assertThrows(AccessDeniedException.class, () -> access.requireVisible(p, 4L));
    }
    @Test void supervisorWithoutTeamFallsBackToSelf() {
        var p = caller("SUPERVISOR"); doReturn(Optional.of(person(1L, null, null, EmployeeStatus.ACTIVE))).when(repository).findById(1L);
        assertEquals(List.of(1L), access.visibleIds(p)); verify(repository, never()).findBySupervisorId(any());
    }
    @Test void managerReadsInactiveEmployeesToo() {
        var p = caller("MANAGER_ADMIN"); doReturn(List.of(person(2L, null, null, EmployeeStatus.INACTIVE))).when(repository).findAll();
        assertEquals(List.of(2L), access.visibleIds(p));
    }
}
