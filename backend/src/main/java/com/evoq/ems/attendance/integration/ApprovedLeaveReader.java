package com.evoq.ems.attendance.integration;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ApprovedLeaveReader {

    private final JdbcTemplate jdbc;

    public ApprovedLeaveReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LeaveConflict> approvedConflicts(Long employeeId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT start_date, end_date FROM leave_request
                WHERE employee_id = ? AND status = 'APPROVED'
                  AND start_date <= ? AND end_date >= ?
                ORDER BY start_date, end_date
                """, (rs, row) -> new LeaveConflict(
                rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()),
                employeeId, java.sql.Date.valueOf(to), java.sql.Date.valueOf(from));
    }

    public List<EmployeeLeave> approvedForEmployees(java.util.Collection<Long> employeeIds, LocalDate from, LocalDate to) {
        if (employeeIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(employeeIds.size(), "?"));
        java.util.List<Object> args = new java.util.ArrayList<>(employeeIds);
        args.add(java.sql.Date.valueOf(to)); args.add(java.sql.Date.valueOf(from));
        return jdbc.query("SELECT employee_id, start_date, end_date FROM leave_request WHERE status = 'APPROVED'"
                + " AND employee_id IN (" + placeholders + ") AND start_date <= ? AND end_date >= ?",
                (rs, row) -> new EmployeeLeave(rs.getLong("employee_id"), rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate()), args.toArray());
    }

    public record EmployeeLeave(Long employeeId, LocalDate startDate, LocalDate endDate) { }

    public record LeaveConflict(LocalDate startDate, LocalDate endDate) { }
}
