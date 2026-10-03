package com.evoq.ems.attendance.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTeamReader {

    private final JdbcTemplate jdbc;

    public EmployeeTeamReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EmployeeInfo> employee(Long id) {
        List<EmployeeInfo> rows = jdbc.query("""
                SELECT employee_id, CONCAT(first_name, ' ', last_name) AS full_name,
                       team_id, supervisor_id, status
                FROM employee WHERE employee_id = ?
                """, (rs, row) -> new EmployeeInfo(rs.getLong("employee_id"),
                rs.getString("full_name"), rs.getObject("team_id", Long.class),
                rs.getObject("supervisor_id", Long.class), rs.getString("status")), id);
        return rows.stream().findFirst();
    }

    public Optional<TeamInfo> team(Long id) {
        List<TeamInfo> rows = jdbc.query("SELECT team_id, name FROM team_project WHERE team_id = ?",
                (rs, row) -> new TeamInfo(rs.getLong("team_id"), rs.getString("name")), id);
        return rows.stream().findFirst();
    }

    public List<TeamInfo> allTeams() {
        return jdbc.query("SELECT team_id, name FROM team_project ORDER BY name, team_id",
                (rs, row) -> new TeamInfo(rs.getLong("team_id"), rs.getString("name")));
    }

    public List<Long> activeDirectReportIds(Long supervisorId, Long teamId) {
        return jdbc.queryForList("""
                SELECT employee_id FROM employee
                WHERE supervisor_id = ? AND team_id = ? AND status = 'ACTIVE'
                ORDER BY employee_id
                """, Long.class, supervisorId, teamId);
    }

    public List<EmployeeInfo> activeDirectReports(Long supervisorId, Long teamId) {
        return jdbc.query("""
                SELECT employee_id, CONCAT(first_name, ' ', last_name) AS full_name,
                       team_id, supervisor_id, status
                FROM employee
                WHERE supervisor_id = ? AND team_id = ? AND status = 'ACTIVE'
                ORDER BY last_name, first_name, employee_id
                """, (rs, row) -> new EmployeeInfo(rs.getLong("employee_id"),
                rs.getString("full_name"), rs.getObject("team_id", Long.class),
                rs.getObject("supervisor_id", Long.class), rs.getString("status")), supervisorId, teamId);
    }

    public List<EmployeeInfo> activeEmployees() {
        return jdbc.query("""
                SELECT employee_id, CONCAT(first_name, ' ', last_name) AS full_name,
                       team_id, supervisor_id, status
                FROM employee WHERE status = 'ACTIVE' ORDER BY last_name, first_name, employee_id
                """, (rs, row) -> new EmployeeInfo(rs.getLong("employee_id"),
                rs.getString("full_name"), rs.getObject("team_id", Long.class),
                rs.getObject("supervisor_id", Long.class), rs.getString("status")));
    }

    public List<EmployeeInfo> allEmployees() {
        return jdbc.query("""
                SELECT employee_id, CONCAT(first_name, ' ', last_name) AS full_name,
                       team_id, supervisor_id, status
                FROM employee ORDER BY last_name, first_name, employee_id
                """, (rs, row) -> new EmployeeInfo(rs.getLong("employee_id"),
                rs.getString("full_name"), rs.getObject("team_id", Long.class),
                rs.getObject("supervisor_id", Long.class), rs.getString("status")));
    }

    public void lockEmployee(Long id) {
        jdbc.queryForObject("SELECT employee_id FROM employee WHERE employee_id = ? FOR UPDATE", Long.class, id);
    }

    public record EmployeeInfo(Long id, String name, Long teamId, Long supervisorId, String status) {
        public boolean active() { return "ACTIVE".equals(status); }
    }

    public record TeamInfo(Long id, String name) { }
}
