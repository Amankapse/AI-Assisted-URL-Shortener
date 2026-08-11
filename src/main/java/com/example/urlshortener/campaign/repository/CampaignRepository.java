package com.example.urlshortener.campaign.repository;

import com.example.urlshortener.campaign.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID> {
    @Query("select c from CampaignEntity c where c.id = :id and c.workspace.id = :workspaceId and c.deleted = false")
    Optional<CampaignEntity> findActiveByIdAndWorkspaceId(@Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query("select c from CampaignEntity c where c.workspace.id = :workspaceId and c.deleted = false order by c.createdAt desc, c.id desc")
    List<CampaignEntity> findActiveByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("select c from CampaignEntity c where c.workspace.id = :workspaceId and c.id in :ids")
    List<CampaignEntity> findByWorkspaceIdAndIdIn(@Param("workspaceId") UUID workspaceId, @Param("ids") List<UUID> ids);

    @Query("select count(c.id) > 0 from CampaignEntity c where c.workspace.id = :workspaceId and c.normalizedName = :normalizedName and c.deleted = false")
    boolean existsActiveByWorkspaceIdAndNormalizedName(@Param("workspaceId") UUID workspaceId, @Param("normalizedName") String normalizedName);

    @Modifying
    @Query("update ShortUrlEntity url set url.campaign = null where url.workspace.id = :workspaceId and url.campaign.id = :campaignId")
    int detachUrls(@Param("workspaceId") UUID workspaceId, @Param("campaignId") UUID campaignId);
}
