package com.evoq.ems.attendance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(AttendanceReconciliationJob.class);
    private final ScheduleEntryRepository entries;
    private final AttendanceReconciliationService reconciliation;
    private final Clock clock;

    public AttendanceReconciliationJob(ScheduleEntryRepository entries,
            AttendanceReconciliationService reconciliation, Clock attendanceClock) {
        this.entries = entries;
        this.reconciliation = reconciliation;
        this.clock = attendanceClock;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reconcileRecentShifts() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        for (var entry : entries.findPendingAttendanceBetweenDates(today.minusDays(366), today)) {
            try {
                reconciliation.reconcile(entry.getEmployeeId(), entry.getWorkDate(), now);
            } catch (Exception exception) {
                log.error("Could not reconcile attendance for employee {} on {}",
                        entry.getEmployeeId(), entry.getWorkDate(), exception);
            }
        }
    }
}
