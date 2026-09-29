package com.evoq.ems.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.evoq.ems.employee.domain.Employee;
import com.evoq.ems.employee.domain.EmployeeStatus;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Employee> findByEmailIgnoreCase(String email);

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByTeamId(Long teamId);

    List<Employee> findBySupervisorId(Long supervisorId);

    List<Employee> findBySupervisorIdAndStatus(Long supervisorId, EmployeeStatus status);

    @Query("""
            SELECT e FROM Employee e
            WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY e.lastName, e.firstName
            """)
    List<Employee> searchEmployees(@Param("keyword") String keyword);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :deptId AND e.status = :status")
    long countByDepartmentIdAndStatus(@Param("deptId") Long deptId, @Param("status") EmployeeStatus status);
}
