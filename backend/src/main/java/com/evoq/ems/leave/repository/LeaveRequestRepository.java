package com.evoq.ems.leave.repository;

import com.evoq.ems.leave.domain.LeaveRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT r FROM LeaveRequest r JOIN FETCH r.employee JOIN FETCH r.leaveType WHERE r.employee.id IN :ids ORDER BY r.submittedDate DESC")
    List<LeaveRequest> findForEmployees(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM LeaveRequest r WHERE r.id = :id")
    Optional<LeaveRequest> findLockedById(@Param("id") Long id);

    @Query("""
        SELECT r FROM LeaveRequest r
        JOIN FETCH r.leaveType
        WHERE r.employee.id = :employeeId
        ORDER BY r.submittedDate DESC
    """)
    List<LeaveRequest> findForEmployee(@Param("employeeId") Long employeeId);

    @Query("""
        SELECT r FROM LeaveRequest r
        JOIN FETCH r.leaveType
        JOIN FETCH r.employee
        WHERE r.employee.supervisor.id = :supervisorId
          AND r.status = 'PENDING'
        ORDER BY r.submittedDate ASC
    """)
    List<LeaveRequest> findPendingForSupervisor(@Param("supervisorId") Long supervisorId);

    @Query("""
        SELECT r FROM LeaveRequest r
        JOIN FETCH r.leaveType
        JOIN FETCH r.employee
        ORDER BY r.submittedDate DESC
    """)
    List<LeaveRequest> findAllForManager();

    @Query("""
        SELECT COUNT(r) FROM LeaveRequest r
        WHERE r.employee.id = :employeeId
          AND r.status IN ('PENDING', 'APPROVED')
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
    """)
    long countOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
