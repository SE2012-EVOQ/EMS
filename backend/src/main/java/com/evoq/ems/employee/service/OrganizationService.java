package com.evoq.ems.employee.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evoq.ems.employee.domain.Department;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.employee.domain.TeamProject;
import com.evoq.ems.employee.repository.DepartmentRepository;
import com.evoq.ems.employee.repository.EmployeeRepository;
import com.evoq.ems.employee.repository.TeamProjectRepository;
import com.evoq.ems.employee.web.EmployeeDtos.DepartmentDto;
import com.evoq.ems.employee.web.EmployeeDtos.DepartmentRequest;
import com.evoq.ems.employee.web.EmployeeDtos.TeamDto;
import com.evoq.ems.employee.web.EmployeeDtos.TeamRequest;
import com.evoq.ems.employee.web.EmployeeModuleException;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final TeamProjectRepository teamProjectRepository;
    private final EmployeeRepository employeeRepository;

    public OrganizationService(DepartmentRepository departmentRepository,
                               TeamProjectRepository teamProjectRepository,
                               EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.teamProjectRepository = teamProjectRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toDepartmentDto)
                .toList();
    }

    public DepartmentDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Department not found with ID: " + id));
        return toDepartmentDto(dept);
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentRequest request) {
        String trimmedName = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw EmployeeModuleException.conflict("Department with name '" + trimmedName + "' already exists");
        }
        Department dept = new Department(trimmedName, request.description() != null ? request.description().trim() : null);
        Department saved = departmentRepository.save(dept);
        return toDepartmentDto(saved);
    }

    public List<TeamDto> getAllTeams() {
        return teamProjectRepository.findAll().stream()
                .map(this::toTeamDto)
                .toList();
    }

    public TeamDto getTeamById(Long id) {
        TeamProject team = teamProjectRepository.findById(id)
                .orElseThrow(() -> EmployeeModuleException.notFound("Team/Project not found with ID: " + id));
        return toTeamDto(team);
    }

    @Transactional
    public TeamDto createTeam(TeamRequest request) {
        String trimmedName = request.name().trim();
        if (teamProjectRepository.existsByNameIgnoreCase(trimmedName)) {
            throw EmployeeModuleException.conflict("Team/Project with name '" + trimmedName + "' already exists");
        }
        TeamProject team = new TeamProject(trimmedName, request.description() != null ? request.description().trim() : null);
        TeamProject saved = teamProjectRepository.save(team);
        return toTeamDto(saved);
    }

    private DepartmentDto toDepartmentDto(Department dept) {
        long count = employeeRepository.countByDepartmentIdAndStatus(dept.getId(), EmployeeStatus.ACTIVE);
        return new DepartmentDto(dept.getId(), dept.getName(), dept.getDescription(), count);
    }

    private TeamDto toTeamDto(TeamProject team) {
        return new TeamDto(team.getId(), team.getName(), team.getDescription());
    }
}
