package org.example.data.repository;

import org.example.common.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Integer> {
    Optional<AuthToken> findByJti(String jti);

    boolean existsByJtiAndRevokedFalseAndExpiresAtAfter(String jti, LocalDateTime now);

    @Transactional
    @Modifying
    @Query("delete from AuthToken at where at.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}