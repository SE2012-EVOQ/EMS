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

    public record LeaveConflict(LocalDate startDate, LocalDate endDate) { }
}
