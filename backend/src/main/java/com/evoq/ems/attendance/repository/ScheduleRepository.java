package com.evoq.ems.attendance.repository;

import java.time.LocalDate;
import java.util.List;

import com.evoq.ems.attendance.domain.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select schedule from Schedule schedule where schedule.id = :id")
    Optional<Schedule> findLockedById(@Param("id") Long id);

    List<Schedule> findByTeamIdAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartAscIdAsc(
            Long teamId, LocalDate from, LocalDate to);

    List<Schedule> findByTeamIdAndStatusAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartAscIdAsc(
            Long teamId, Schedule.Status status, LocalDate from, LocalDate to);
}
