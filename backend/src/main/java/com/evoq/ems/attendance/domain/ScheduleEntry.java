package com.evoq.ems.attendance.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Attendance-owned mapping of the frozen schedule_entry table. */
@Entity
@Table(name = "schedule_entry")
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_entry_id")
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "notes", length = 255)
    private String notes;

    protected ScheduleEntry() { }

    public ScheduleEntry(Long scheduleId, Long employeeId, LocalDate workDate,
            LocalTime startTime, LocalTime endTime, String notes) {
        update(scheduleId, employeeId, workDate, startTime, endTime, notes);
    }

    public void update(Long scheduleId, Long employeeId, LocalDate workDate,
            LocalTime startTime, LocalTime endTime, String notes) {
        this.scheduleId = scheduleId;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getScheduleId() { return scheduleId; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getNotes() { return notes; }
}
