package com.example.urlshortener.apikey.repository;

import com.example.urlshortener.apikey.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {
    Optional<ApiKeyEntity> findByKeyPrefix(String keyPrefix);

    @Query("select key from ApiKeyEntity key where key.workspace.id = :workspaceId order by key.createdAt desc")
    List<ApiKeyEntity> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("select key from ApiKeyEntity key join fetch key.workspace where key.id = :id and key.workspace.id = :workspaceId")
    Optional<ApiKeyEntity> findByIdAndWorkspaceId(@Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("update ApiKeyEntity key set key.lastUsedAt = :now where key.id = :id and (key.lastUsedAt is null or key.lastUsedAt <= :threshold)")
    int updateLastUsedIfOlder(@Param("id") UUID id, @Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);
}
