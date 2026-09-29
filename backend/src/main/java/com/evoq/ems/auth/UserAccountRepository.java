package com.evoq.ems.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<UserAccount> findByUsername(String username);

    @Modifying
    @Transactional
    @Query("update UserAccount account set account.passwordHash = :encoded where account.passwordHash = :placeholder")
    int replacePlaceholderHashes(@Param("placeholder") String placeholder, @Param("encoded") String encoded);
}
