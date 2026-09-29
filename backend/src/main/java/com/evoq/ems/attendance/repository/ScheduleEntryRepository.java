package com.evoq.ems.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.evoq.ems.attendance.domain.Schedule;
import com.evoq.ems.attendance.domain.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    List<ScheduleEntry> findByScheduleIdOrderByWorkDateAscStartTimeAscIdAsc(Long scheduleId);

    List<ScheduleEntry> findByScheduleIdInOrderByWorkDateAscStartTimeAscIdAsc(Collection<Long> scheduleIds);

    @Query("""
            select count(entry) from ScheduleEntry entry
            where entry.employeeId = :employeeId and entry.workDate = :workDate
              and entry.scheduleId <> :scheduleId
              and entry.startTime < :endTime and entry.endTime > :startTime
            """)
    long countOverlapsOutsideSchedule(@Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate, @Param("scheduleId") Long scheduleId,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);

    @Query("""
            select count(entry) from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where entry.employeeId = :employeeId and entry.workDate = :workDate
              and entry.scheduleId <> :scheduleId and schedule.status = :status
            """)
    long countEntriesOutsideSchedule(@Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate, @Param("scheduleId") Long scheduleId,
            @Param("status") Schedule.Status status);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where entry.employeeId = :employeeId and entry.workDate between :from and :to
              and schedule.status = :status
            order by entry.workDate asc, entry.startTime asc, entry.id asc
            """)
    List<ScheduleEntry> findEmployeeEntries(@Param("employeeId") Long employeeId,
            @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("status") Schedule.Status status);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where entry.employeeId = :employeeId and entry.workDate = :workDate
              and schedule.status = com.evoq.ems.attendance.domain.Schedule.Status.PUBLISHED
            order by entry.startTime asc, entry.id asc
            """)
    List<ScheduleEntry> findPublishedEntriesForEmployeeDate(@Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where entry.employeeId = :employeeId and entry.workDate = :workDate
              and schedule.status = :status and schedule.teamId = :teamId
            order by entry.startTime asc, entry.id asc
            """)
    List<ScheduleEntry> findTeamEntriesForEmployeeDate(@Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate, @Param("teamId") Long teamId,
            @Param("status") Schedule.Status status);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where entry.employeeId = :employeeId and entry.workDate = :workDate
              and schedule.status = com.evoq.ems.attendance.domain.Schedule.Status.PUBLISHED
            """)
    Optional<ScheduleEntry> findSinglePublishedEntry(@Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where schedule.teamId = :teamId and schedule.status = :status
              and entry.workDate between :from and :to
            order by entry.workDate asc, entry.startTime asc, entry.id asc
            """)
    List<ScheduleEntry> findTeamEntries(@Param("teamId") Long teamId,
            @Param("status") Schedule.Status status, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select entry from ScheduleEntry entry join Schedule schedule on schedule.id = entry.scheduleId
            where schedule.id = :scheduleId and schedule.status = com.evoq.ems.attendance.domain.Schedule.Status.PUBLISHED
            """)
    List<ScheduleEntry> findEntriesInPublishedSchedule(@Param("scheduleId") Long scheduleId);
}
