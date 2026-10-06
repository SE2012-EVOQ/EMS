package com.evoq.ems.attendance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import com.evoq.ems.attendance.integration.ApprovedLeaveReader;
import com.evoq.ems.attendance.integration.EmployeeTeamReader;
import com.evoq.ems.attendance.repository.AttendanceRecordRepository;
import com.evoq.ems.attendance.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceReconciliationService {

    private final ScheduleEntryRepository entries;
    private final AttendanceRecordRepository records;
    private final EmployeeTeamReader people;
    private final ApprovedLeaveReader leaves;

    public AttendanceReconciliationService(ScheduleEntryRepository entries,
            AttendanceRecordRepository records, EmployeeTeamReader people, ApprovedLeaveReader leaves) {
        this.entries = entries;
        this.records = records;
        this.people = people;
        this.leaves = leaves;
    }

    @Transactional
    public void reconcile(Long employeeId, LocalDate workDate, LocalDateTime now) {
        people.lockEmployee(employeeId);
        var scheduled = entries.findPublishedEntriesForEmployeeDate(employeeId, workDate);
        if (scheduled.size() != 1) return;
        ScheduleEntry entry = scheduled.getFirst();
        if (now.isBefore(workDate.atTime(entry.getEndTime()))) return;

        AttendanceRecord record = records.findByEmployeeIdAndAttendanceDate(employeeId, workDate).orElse(null);
        if (record == null) {
            if (!leaves.approvedConflicts(employeeId, workDate, workDate).isEmpty()) return;
            records.save(new AttendanceRecord(employeeId, workDate, AttendanceRecord.Status.ABSENT,
                    null, null, BigDecimal.ZERO.setScale(2), "Automatically marked absent after scheduled shift"));
            return;
        }
        if (record.getCheckInTime() != null && record.getCheckOutTime() == null
                && entry.getEndTime().isAfter(record.getCheckInTime())) {
            record.checkOut(entry.getEndTime(), AttendanceService.calculateHours(
                    record.getCheckInTime(), entry.getEndTime()));
            records.save(record);
        }
    }
}
