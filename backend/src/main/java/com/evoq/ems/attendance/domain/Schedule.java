package com.evoq.ems.attendance.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Attendance-owned mapping of the frozen schedule table. */
@Entity
@Table(name = "schedule")
public class Schedule {

    public enum Status { DRAFT, PUBLISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    protected Schedule() { }

    public Schedule(Long teamId, LocalDate periodStart, LocalDate periodEnd) {
        update(teamId, periodStart, periodEnd);
        this.status = Status.DRAFT;
    }

    public void update(Long teamId, LocalDate periodStart, LocalDate periodEnd) {
        this.teamId = teamId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public void publish() { this.status = Status.PUBLISHED; }

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public Status getStatus() { return status; }
}
