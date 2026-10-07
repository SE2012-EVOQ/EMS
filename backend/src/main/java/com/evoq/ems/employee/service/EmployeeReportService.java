package com.evoq.ems.employee.service;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.employee.service.EmployeeService;
import com.evoq.ems.employee.service.EmployeeAccess;
import com.evoq.ems.employee.domain.EmployeeStatus;
import com.evoq.ems.common.ModuleReport;
import static com.evoq.ems.common.ModuleReport.text;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true)
public class EmployeeReportService {
    private final EmployeeService employees;
    private final OrganizationService organization;
    public EmployeeReportService(EmployeeService employees, OrganizationService organization) {
        this.employees = employees; this.organization = organization;
    }
    public ModuleReport report(AccountPrincipal caller,
            Long departmentId, Long teamId,
            EmployeeStatus status) {
        var rows = employees.getAllEmployees(caller, departmentId, teamId, status, null);
        Map<String, Number> summary = new LinkedHashMap<>();
        summary.put("Employees in scope", rows.size());
        summary.put("Active employees", rows.stream().filter(e -> e.status() == EmployeeStatus.ACTIVE).count());
        summary.put("Inactive employees", rows.stream().filter(e -> e.status() == EmployeeStatus.INACTIVE).count());
        if (EmployeeAccess.manager(caller)) {
            summary.put("Departments configured", organization.getAllDepartments().size());
            summary.put("Teams / projects configured", organization.getAllTeams().size());
        } else {
            summary.put("Departments represented", rows.stream().filter(e -> e.department() != null).map(e -> e.department().id()).distinct().count());
            summary.put("Teams represented", rows.stream().filter(e -> e.team() != null).map(e -> e.team().id()).distinct().count());
        }
        return new ModuleReport(EmployeeAccess.manager(caller) ? "ORGANIZATION" : caller.getRole().equals("SUPERVISOR") ? "SELF_AND_TEAM" : "MINE",
                summary, List.of(new ModuleReport.Table("Employee records", List.of("Employee ID", "Name", "Email", "Hire date", "Job title", "Status", "Department", "Team", "Supervisor"),
                rows.stream().map(e -> List.of(text(e.id()), text(e.fullName()), text(e.email()), text(e.hireDate()), text(e.jobTitle()), text(e.status()),
                        e.department() == null ? "" : text(e.department().name()), e.team() == null ? "" : text(e.team().name()), e.supervisor() == null ? "" : text(e.supervisor().fullName()))).toList())));
    }
}
