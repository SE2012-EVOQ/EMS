package com.evoq.ems.attendance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_record")
public class AttendanceRecord {

    public enum Status { PRESENT, LATE, ABSENT, LEAVE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "working_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal workingHours;

    @Column(name = "notes", length = 255)
    private String notes;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(Long employeeId, LocalDate attendanceDate, Status status,
            LocalTime checkInTime, LocalTime checkOutTime, BigDecimal workingHours, String notes) {
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        correct(status, checkInTime, checkOutTime, workingHours, notes);
    }

    public void correct(Status status,
            LocalTime checkInTime, LocalTime checkOutTime, BigDecimal workingHours, String notes) {
        this.status = status;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.workingHours = workingHours;
        this.notes = notes;
    }

    public void checkOut(LocalTime time, BigDecimal hours) {
        this.checkOutTime = time;
        this.workingHours = hours;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public Status getStatus() { return status; }
    public BigDecimal getWorkingHours() { return workingHours; }
    public String getNotes() { return notes; }
}
