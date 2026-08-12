package com.example.urlshortener.site.repository;

import com.example.urlshortener.site.entity.SiteSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SiteSettingsRepository extends JpaRepository<SiteSettingsEntity, UUID> {
}
