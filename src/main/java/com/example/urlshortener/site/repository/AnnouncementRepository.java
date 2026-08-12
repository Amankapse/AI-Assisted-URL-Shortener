package com.example.urlshortener.site.repository;

import com.example.urlshortener.site.entity.AnnouncementAudience;
import com.example.urlshortener.site.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {
    List<AnnouncementEntity> findAllByOrderByCreatedAtDesc();

    List<AnnouncementEntity> findByEnabledTrueAndAudienceInAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByCreatedAtDesc(
            Collection<AnnouncementAudience> audiences,
            LocalDateTime startAt,
            LocalDateTime endAt);
}
