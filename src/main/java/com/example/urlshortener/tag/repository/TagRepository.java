package com.example.urlshortener.tag.repository;

import com.example.urlshortener.tag.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<TagEntity, UUID> {
    @Query("select t from TagEntity t where t.workspace.id = :workspaceId and t.normalizedName = :normalizedName")
    Optional<TagEntity> findByWorkspaceIdAndNormalizedName(@Param("workspaceId") UUID workspaceId, @Param("normalizedName") String normalizedName);

    @Query("select t from TagEntity t where t.workspace.id = :workspaceId order by t.normalizedName")
    List<TagEntity> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("select url.id, tag from ShortUrlEntity url join url.tags tag where url.id in :urlIds")
    List<Object[]> findTagsByUrlIds(@Param("urlIds") Collection<UUID> urlIds);
}
