package com.example.urlshortener.user.service;

import com.example.urlshortener.user.entity.UserRole;

import java.util.UUID;

public record OwnerIdentity(UUID userId, String email, UserRole role) {
}
