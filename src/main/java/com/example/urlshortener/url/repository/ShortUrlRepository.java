package com.example.urlshortener.url.repository;

import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity, UUID> {
    @Query("select url from ShortUrlEntity url where url.shortCode = :shortCode and url.deleted = false")
    Optional<ShortUrlEntity> findByShortCode(@Param("shortCode") String shortCode);

    Optional<ShortUrlEntity> findByCustomAlias(String customAlias);

    @Query("select url from ShortUrlEntity url where url.id = :id and url.owner = :owner and url.deleted = false")
    Optional<ShortUrlEntity> findByIdAndOwner(@Param("id") UUID id, @Param("owner") UserEntity owner);

    @Query("select url from ShortUrlEntity url where url.owner = :owner and url.deleted = false")
    Page<ShortUrlEntity> findByOwner(@Param("owner") UserEntity owner, Pageable pageable);

    boolean existsByShortCode(String shortCode);
    boolean existsByCustomAlias(String customAlias);
    boolean existsByIdAndOwner(UUID id, UserEntity owner);

    @Query("select count(url.id) from ShortUrlEntity url where url.owner = :owner and url.createdAt >= :start")
    long countCreatedByOwnerSince(@Param("owner") UserEntity owner, @Param("start") LocalDateTime start);

    @Query("select count(url.id) from ShortUrlEntity url where url.owner = :owner and url.deleted = false and url.enabled = true and url.blocked = false and (url.expiresAt is null or url.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveByOwner(@Param("owner") UserEntity owner);

    @Query("select count(url.id) from ShortUrlEntity url where url.owner = :owner and url.customAlias is not null and url.createdAt >= :start")
    long countCustomAliasesByOwnerSince(@Param("owner") UserEntity owner, @Param("start") LocalDateTime start);

    @Query("select count(url.id) from ShortUrlEntity url where url.enabled = :enabled and url.deleted = false")
    long countByEnabled(@Param("enabled") boolean enabled);

    @Query("select count(url.id) from ShortUrlEntity url where url.deleted = false and url.enabled = true and url.blocked = false and (url.expiresAt is null or url.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveLinks();

    @Query("select count(url.id) from ShortUrlEntity url where url.deleted = false and url.expiresAt is not null and url.expiresAt <= CURRENT_TIMESTAMP")
    long countExpiredLinks();

    @Query("select count(url.id) from ShortUrlEntity url where url.deleted = false")
    long countNotDeleted();
}
