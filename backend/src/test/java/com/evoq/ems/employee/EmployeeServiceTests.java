package com.evoq.ems.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.evoq.ems.auth.Role;
import com.evoq.ems.auth.RoleRepository;
import com.evoq.ems.auth.UserAccount;
import com.evoq.ems.auth.UserAccountRepository;
import com.evoq.ems.employee.domain.Department;
import com.evoq.ems.employee.domain.Employee;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.repository.DepartmentRepository;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.employee.repository.TeamProjectRepository;
import com.evoq.ems.employee.service.EmployeeService;
import com.evoq.ems.employee.web.EmployeeDtos.CreateEmployeeRequest;
import com.evoq.ems.employee.web.EmployeeDtos.EmployeeResponse;
import com.evoq.ems.employee.web.EmployeeDtos.UpdateOfficialInfoRequest;
import com.evoq.ems.employee.web.EmployeeDtos.UpdatePersonalContactRequest;
import com.evoq.ems.employee.web.EmployeeModuleException;

class EmployeeServiceTests {

    private final EmployeeRepository employeeRepo = mock(EmployeeRepository.class);
    private final DepartmentRepository deptRepo = mock(DepartmentRepository.class);
    private final TeamProjectRepository teamRepo = mock(TeamProjectRepository.class);
    private final UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);
    private final RoleRepository roleRepo = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private EmployeeService employeeService;

    @BeforeEach
    void setup() {
        employeeService = new EmployeeService(
                employeeRepo,
                deptRepo,
                teamRepo,
                userAccountRepo,
                roleRepo,
                passwordEncoder
        );
    }

    @Test
    void createEmployee_successfullyCreatesEmployee() {
        Department dept = new Department("Engineering", "Software dev");
        when(deptRepo.findById(1L)).thenReturn(Optional.of(dept));
        when(employeeRepo.existsByEmailIgnoreCase("john.doe@evoq.com")).thenReturn(false);
        when(employeeRepo.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, null, null,
                "John", "Doe", "john.doe@evoq.com",
                "0771234567", "Colombo",
                LocalDate.of(2026, 1, 1),
                "Software Engineer",
                EmployeeStatus.ACTIVE,
                false, null, null, null
        );

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john.doe@evoq.com", response.email());
        assertEquals("Engineering", response.department().name());
    }

    @Test
    void createEmployee_throwsConflictOnDuplicateEmail() {
        when(employeeRepo.existsByEmailIgnoreCase("duplicate@evoq.com")).thenReturn(true);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, null, null,
                "Jane", "Doe", "duplicate@evoq.com",
                null, null,
                LocalDate.of(2026, 1, 1),
                "QA Engineer",
                EmployeeStatus.ACTIVE,
                false, null, null, null
        );

        EmployeeModuleException ex = assertThrows(EmployeeModuleException.class, () ->
                employeeService.createEmployee(request));

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void updateOfficialInfo_preventsSelfSupervision() {
        Department dept = new Department("Engineering", "Software dev");
        when(deptRepo.findById(1L)).thenReturn(Optional.of(dept));

        Employee emp = new Employee(dept, null, null, "John", "Doe",
                "john@evoq.com", null, null, LocalDate.now(), "Engineer", EmployeeStatus.ACTIVE);
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(emp));

        UpdateOfficialInfoRequest request = new UpdateOfficialInfoRequest(
                1L, null, 5L, // Supervisor ID equals Employee ID
                "Senior Engineer", EmployeeStatus.ACTIVE, null
        );

        EmployeeModuleException ex = assertThrows(EmployeeModuleException.class, () ->
                employeeService.updateOfficialInfo(5L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.status());
        assertTrue(ex.getMessage().contains("cannot be their own supervisor"));
    }

    @Test
    void updatePersonalContact_successfullyUpdatesPhoneAndAddress() {
        Department dept = new Department("Engineering", "Software dev");
        Employee emp = new Employee(dept, null, null, "John", "Doe",
                "john@evoq.com", "0112223344", "Old Address", LocalDate.now(), "Engineer", EmployeeStatus.ACTIVE);
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(emp));
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePersonalContactRequest request = new UpdatePersonalContactRequest("0779998877", "New Address");
        EmployeeResponse response = employeeService.updatePersonalContact(10L, request);

        assertEquals("0779998877", response.phone());
        assertEquals("New Address", response.address());
    }

    @Test
    void changeEmployeeStatus_deactivatesLinkedUserAccount() {
        Department dept = new Department("Engineering", "Software dev");
        Employee emp = new Employee(dept, null, null, "John", "Doe",
                "john@evoq.com", null, null, LocalDate.now(), "Engineer", EmployeeStatus.ACTIVE);
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(emp));
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Role role = mock(Role.class);
        UserAccount acc = new UserAccount(10L, role, "john.doe", "hash", true);
        when(userAccountRepo.findByEmployeeId(10L)).thenReturn(Optional.of(acc));

        EmployeeResponse response = employeeService.changeEmployeeStatus(10L, EmployeeStatus.INACTIVE);

        assertEquals(EmployeeStatus.INACTIVE, response.status());
        assertEquals(false, acc.isActive());
    }
}
