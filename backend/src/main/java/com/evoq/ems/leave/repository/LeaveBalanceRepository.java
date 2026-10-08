package com.evoq.ems.leave.repository;

import com.evoq.ems.leave.domain.LeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @Query("SELECT b FROM LeaveBalance b JOIN FETCH b.employee JOIN FETCH b.leaveType WHERE b.employee.id IN :ids ORDER BY b.employee.id, b.leaveType.name")
    List<LeaveBalance> findForEmployees(@Param("ids") List<Long> ids);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeId(
            Long employeeId,
            Long leaveTypeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM LeaveBalance b WHERE b.employee.id = :employeeId AND b.leaveType.id = :leaveTypeId")
    Optional<LeaveBalance> findLockedForEmployeeType(@Param("employeeId") Long employeeId,
            @Param("leaveTypeId") Long leaveTypeId);

    @Query("""
        SELECT b FROM LeaveBalance b
        JOIN FETCH b.leaveType
        WHERE b.employee.id = :employeeId
        ORDER BY b.leaveType.name
    """)
    List<LeaveBalance> findForEmployee(@Param("employeeId") Long employeeId);
}
