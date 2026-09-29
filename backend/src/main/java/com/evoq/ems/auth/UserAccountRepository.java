package com.evoq.ems.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Optional<UserAccount> findByEmployeeId(Long employeeId);

    boolean existsByUsername(String username);

    boolean existsByEmployeeId(Long employeeId);
}
