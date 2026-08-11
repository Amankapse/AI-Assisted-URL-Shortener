package com.example.urlshortener.tag.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TagResponse(UUID id, String name, String normalizedName, LocalDateTime createdAt) {
}
