package com.evoq.ems.attendance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Service;

@Service
public class ApprovedLeaveScheduleCoordinator {

    private final ScheduleEntryRepository entries;
    private final AttendanceRecordRepository attendance;
    private final EmployeeTeamReader people;
    private final Clock clock;

    public ApprovedLeaveScheduleCoordinator(ScheduleEntryRepository entries,
            AttendanceRecordRepository attendance, EmployeeTeamReader people, Clock attendanceClock) {
        this.entries = entries;
        this.attendance = attendance;
        this.people = people;
        this.clock = attendanceClock;
    }

    /** Called inside the leave approval transaction. */
    public boolean removeFutureShifts(Long employeeId, LocalDate start, LocalDate end) {
        people.lockEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<ScheduleEntry> future = entries.findEmployeeEntries(employeeId, start, end, Schedule.Status.PUBLISHED)
                .stream().filter(entry -> entry.getWorkDate().atTime(entry.getStartTime()).isAfter(now)).toList();
        for (ScheduleEntry entry : future) {
            if (attendance.existsByEmployeeIdAndAttendanceDate(employeeId, entry.getWorkDate())) return false;
        }
        entries.deleteAll(future);
        return true;
    }
}
