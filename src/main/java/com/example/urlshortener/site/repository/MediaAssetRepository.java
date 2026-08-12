package com.example.urlshortener.site.repository;

import com.example.urlshortener.site.entity.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {
    List<MediaAssetEntity> findAllByOrderByCreatedAtDesc();
}
