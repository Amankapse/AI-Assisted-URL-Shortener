package com.example.urlshortener.auth.repository;

import com.example.urlshortener.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenDigest(String tokenDigest);
    List<RefreshTokenEntity> findByFamilyId(UUID familyId);
    long countByFamilyIdAndRevokedAtIsNull(UUID familyId);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now where token.familyId = :familyId and token.revokedAt is null")
    int revokeActiveFamily(@Param("familyId") UUID familyId, @Param("now") LocalDateTime now);
}
