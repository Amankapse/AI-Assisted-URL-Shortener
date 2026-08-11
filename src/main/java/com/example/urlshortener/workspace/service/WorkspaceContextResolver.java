package com.example.urlshortener.workspace.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.security.AuthenticatedActor;
import com.example.urlshortener.security.AuthenticatedActorProvider;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.workspace.entity.WorkspaceMembershipEntity;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceContextResolver {
    public static final String WORKSPACE_HEADER = "X-Workspace-ID";

    private final CurrentOwnerProvider currentOwnerProvider;
    private final AuthenticatedActorProvider actorProvider;
    private final WorkspaceMembershipRepository membershipRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceContextResolver(CurrentOwnerProvider currentOwnerProvider,
                                    AuthenticatedActorProvider actorProvider,
                                    WorkspaceMembershipRepository membershipRepository,
                                    WorkspaceAuthorizationService authorizationService) {
        this.currentOwnerProvider = currentOwnerProvider;
        this.actorProvider = actorProvider;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public WorkspaceContext resolveForLinkReader(String headerValue) {
        return authorizationService.requireLinkReader(resolveWorkspaceId(headerValue));
    }

    @Transactional(readOnly = true)
    public WorkspaceContext resolveForLinkWriter(String headerValue) {
        return authorizationService.requireLinkWriter(resolveWorkspaceId(headerValue));
    }

    @Transactional(readOnly = true)
    public WorkspaceContext resolveForAnalyticsReader(String headerValue) {
        return authorizationService.requireAnalyticsReader(resolveWorkspaceId(headerValue));
    }

    private UUID resolveWorkspaceId(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                return UUID.fromString(headerValue.trim());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("X-Workspace-ID must be a valid UUID");
            }
        }
        AuthenticatedActor actor = actorProvider.getRequiredActor();
        if (actor.isApiKey()) {
            return actor.workspaceId();
        }
        UUID actorUserId = currentOwnerProvider.getCurrentOwner().userId();
        return membershipRepository.findByUserIdWithWorkspace(actorUserId).stream()
                .findFirst()
                .map(membership -> membership.getWorkspace().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Default workspace not found"));
    }
}
