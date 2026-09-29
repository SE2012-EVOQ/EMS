package com.evoq.ems.leave.repository;

import com.evoq.ems.leave.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByNameIgnoreCase(String name);
}
