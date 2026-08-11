package com.example.urlshortener.workspace.service;

import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.audit.entity.AuditActorType;

import java.util.UUID;

public record WorkspaceContext(UUID workspaceId, UUID actorId, AuditActorType actorType, WorkspaceRole role, WorkspaceEntity workspace) {
    public UUID actorUserId() {
        return actorId;
    }
}
