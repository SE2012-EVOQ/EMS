package com.evoq.ems.attendance.repository;

import java.time.LocalDate;
import java.util.List;

import com.evoq.ems.attendance.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByTeamIdAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartAscIdAsc(
            Long teamId, LocalDate from, LocalDate to);

    List<Schedule> findByTeamIdAndStatusAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartAscIdAsc(
            Long teamId, Schedule.Status status, LocalDate from, LocalDate to);
}
