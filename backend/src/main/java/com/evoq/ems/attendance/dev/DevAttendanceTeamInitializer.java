package com.evoq.ems.attendance.dev;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevAttendanceTeamInitializer {

    private static final String TEAM_NAME = "Development Scheduling Team";
    private final JdbcTemplate jdbc;

    public DevAttendanceTeamInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        long supervisorId = reservedEmployee("demo.supervisor");
        long employeeId = reservedEmployee("demo.employee");
        Long teamId = jdbc.query("SELECT team_id FROM team_project WHERE name = ?",
                (rs, row) -> rs.getLong("team_id"), TEAM_NAME).stream().findFirst().orElse(null);
        if (teamId == null) {
            jdbc.update("INSERT INTO team_project (name, description) VALUES (?, ?)",
                    TEAM_NAME, "Reserved for local attendance and scheduling development");
            teamId = jdbc.queryForObject("SELECT team_id FROM team_project WHERE name = ?", Long.class, TEAM_NAME);
        }
        jdbc.update("UPDATE employee SET team_id = ? WHERE employee_id = ?", teamId, supervisorId);
        jdbc.update("UPDATE employee SET team_id = ?, supervisor_id = ? WHERE employee_id = ?",
                teamId, supervisorId, employeeId);
    }

    private long reservedEmployee(String username) {
        List<Long> ids = jdbc.queryForList("""
                SELECT e.employee_id FROM employee e
                JOIN user_account u ON u.employee_id = e.employee_id
                WHERE u.username = ? AND e.email = ?
                """, Long.class, username, username + "@evoq.invalid");
        if (ids.size() != 1) throw new IllegalStateException("Reserved development account is missing: " + username);
        return ids.getFirst();
    }
}
