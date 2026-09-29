package com.evoq.ems.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(
            Long employeeId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(
            Collection<Long> employeeIds, LocalDate from, LocalDate to);

    List<AttendanceRecord> findByAttendanceDateBetweenOrderByAttendanceDateDescIdDesc(
            LocalDate from, LocalDate to);

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    boolean existsByEmployeeIdAndAttendanceDateAndIdNot(Long employeeId, LocalDate attendanceDate, Long id);

    java.util.Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateAndCheckOutTimeIsNull(Long employeeId, LocalDate attendanceDate);
}
