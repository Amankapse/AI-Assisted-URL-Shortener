package com.example.urlshortener.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public final class ApiKeyDtos {
    private ApiKeyDtos() {
    }

    public static class CreateApiKeyRequest {
        @NotBlank
        @Size(max = 120)
        private String name;

        @NotEmpty
        private Set<String> scopes;

        private LocalDateTime expiresAt;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public void setScopes(Set<String> scopes) {
            this.scopes = scopes;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    public record ApiKeyCreatedResponse(
            UUID id,
            String name,
            String prefix,
            String rawKey,
            Set<String> scopes,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
    }

    public record ApiKeyResponse(
            UUID id,
            String name,
            String prefix,
            Set<String> scopes,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt,
            LocalDateTime lastUsedAt
    ) {
    }
}
