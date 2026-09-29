package com.evoq.ems.employee.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.evoq.ems.employee.domain.TeamProject;

@Repository
public interface TeamProjectRepository extends JpaRepository<TeamProject, Long> {

    Optional<TeamProject> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
