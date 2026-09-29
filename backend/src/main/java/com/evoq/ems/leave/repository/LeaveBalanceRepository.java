package com.evoq.ems.leave.repository;

import com.evoq.ems.leave.domain.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeId(
            Long employeeId,
            Long leaveTypeId
    );

    @Query("""
        SELECT b FROM LeaveBalance b
        JOIN FETCH b.leaveType
        WHERE b.employee.id = :employeeId
        ORDER BY b.leaveType.name
    """)
    List<LeaveBalance> findForEmployee(@Param("employeeId") Long employeeId);
}
