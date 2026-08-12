package com.example.urlshortener.site.repository;

import com.example.urlshortener.site.entity.ContentPageEntity;
import com.example.urlshortener.site.entity.ContentPageKey;
import com.example.urlshortener.site.entity.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPageRepository extends JpaRepository<ContentPageEntity, UUID> {
    Optional<ContentPageEntity> findByPageKey(ContentPageKey pageKey);
    Optional<ContentPageEntity> findByPageKeyAndStatus(ContentPageKey pageKey, ContentStatus status);
    List<ContentPageEntity> findAllByOrderByPageKeyAsc();
}
