package com.evoq.ems.asset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.evoq.ems.asset.domain.*;
import com.evoq.ems.asset.repository.*;
import com.evoq.ems.asset.service.*;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.domain.*;
import com.evoq.ems.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class AssetWorkflowTests {
    final AssetRepository assets = mock(AssetRepository.class);
    final AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final AssetAssignmentService service = new AssetAssignmentService(assignments, assets, employees);
    final AssetService inventory = new AssetService(assets, assignments);
    Employee active() { var e = mock(Employee.class); when(e.getStatus()).thenReturn(EmployeeStatus.ACTIVE); return e; }
    AccountPrincipal caller(String role) { var p = mock(AccountPrincipal.class); when(p.getRole()).thenReturn(role); when(p.getEmployeeId()).thenReturn(1L); return p; }
    @Test void invalidEmployeeNeverChangesAssetOrCreatesAssignment() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.assignAsset(2L, 999L)).getStatusCode().value());
        verifyNoInteractions(assets, assignments);
    }
    @Test void inactiveEmployeeCannotReceiveEquipment() {
        var inactive = mock(Employee.class); when(inactive.getStatus()).thenReturn(EmployeeStatus.INACTIVE);
        when(employees.findLockedById(1L)).thenReturn(Optional.of(inactive));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.assignAsset(2L, 1L)).getStatusCode().value());
        verifyNoInteractions(assets, assignments);
    }
    @Test void existingActiveAssignmentPreventsAssignEvenIfAssetStatusIsInconsistent() {
        var asset = new Asset("Laptop", "Laptop", "SN", "AVAILABLE");
        doReturn(Optional.of(active())).when(employees).findLockedById(1L); when(assets.findLockedById(2L)).thenReturn(Optional.of(asset));
        when(assignments.existsByAssetIdAndAssignmentStatus(2L, "ASSIGNED")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.assignAsset(2L, 1L)).getStatusCode().value());
        assertEquals("AVAILABLE", asset.getStatus()); verify(assignments, never()).save(any());
    }
    @Test void assignmentValidatesThenRecordsLinkAndChangesStatus() {
        var asset = new Asset("Laptop", "Laptop", "SN", "AVAILABLE");
        doReturn(Optional.of(active())).when(employees).findLockedById(1L); when(assets.findLockedById(2L)).thenReturn(Optional.of(asset));
        when(assignments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.assignAsset(2L, 1L);
        assertEquals("ASSIGNED", asset.getStatus()); assertEquals(1L, result.getEmployeeId()); assertEquals(2L, result.getAssetId());
        assertEquals("ASSIGNED", result.getAssignmentStatus()); assertNull(result.getReturnedDate());
    }
    @Test void repeatedReturnIsConflictAndDoesNotMakeAssetAvailableAgain() {
        var asset = new Asset("Laptop", "Laptop", "SN", "ASSIGNED");
        when(assignments.findAssetId(3L)).thenReturn(Optional.of(2L)); when(assets.findLockedById(2L)).thenReturn(Optional.of(asset));
        when(assignments.findLockedById(3L)).thenReturn(Optional.of(new AssetAssignment(2L, 1L, LocalDate.now(), LocalDate.now(), "RETURNED")));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.returnAsset(3L)).getStatusCode().value());
        assertEquals("ASSIGNED", asset.getStatus()); verify(assets, never()).save(any());
    }
    @Test void statusCannotBypassActiveAssignmentOrFabricateAssignment() {
        var asset = new Asset("Laptop", "Laptop", "SN", "ASSIGNED"); asset.setAssetId(2L);
        when(assets.findLockedById(2L)).thenReturn(Optional.of(asset)); when(assignments.existsByAssetIdAndAssignmentStatus(2L, "ASSIGNED")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> inventory.updateStatus(2L, "AVAILABLE")).getStatusCode().value());
        when(assignments.existsByAssetIdAndAssignmentStatus(2L, "ASSIGNED")).thenReturn(false);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> inventory.updateStatus(2L, "ASSIGNED")).getStatusCode().value());
    }
    @Test void employeeAndSupervisorReadOnlyOwnCurrentEquipment() {
        var history = List.of(new AssetAssignment(2L, 1L, LocalDate.now(), null, "ASSIGNED"), new AssetAssignment(3L, 1L, LocalDate.now(), LocalDate.now(), "RETURNED"));
        when(assignments.findByEmployeeId(1L)).thenReturn(history); when(assets.findAllById(List.of(2L))).thenReturn(List.of());
        for (String role : List.of("EMPLOYEE", "SUPERVISOR")) {
            var p = caller(role); inventory.getAllAssets(p);
            assertThrows(AccessDeniedException.class, () -> inventory.getAssetById(p, 8L));
        }
        verify(assets, never()).findAll(); verify(assets, never()).findById(8L);
    }
}
