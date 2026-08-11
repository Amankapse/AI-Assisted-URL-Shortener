package com.example.urlshortener.workspace.service;

import com.example.urlshortener.common.exception.ForbiddenException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.apikey.entity.ApiKeyScope;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.security.AuthenticatedActor;
import com.example.urlshortener.security.AuthenticatedActorProvider;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import com.example.urlshortener.workspace.entity.WorkspaceMembershipEntity;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
public class WorkspaceAuthorizationService {
    private static final EnumSet<WorkspaceRole> LINK_READERS = EnumSet.allOf(WorkspaceRole.class);
    private static final EnumSet<WorkspaceRole> LINK_WRITERS = EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.EDITOR);
    private static final EnumSet<WorkspaceRole> ANALYTICS_READERS = EnumSet.allOf(WorkspaceRole.class);
    private static final EnumSet<WorkspaceRole> MEMBER_MANAGERS = EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);

    private final CurrentOwnerProvider currentOwnerProvider;
    private final AuthenticatedActorProvider actorProvider;
    private final WorkspaceMembershipRepository membershipRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceAuthorizationService(CurrentOwnerProvider currentOwnerProvider,
                                         AuthenticatedActorProvider actorProvider,
                                         WorkspaceMembershipRepository membershipRepository,
                                         WorkspaceRepository workspaceRepository) {
        this.currentOwnerProvider = currentOwnerProvider;
        this.actorProvider = actorProvider;
        this.membershipRepository = membershipRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireLinkReader(UUID workspaceId) {
        AuthenticatedActor actor = actorProvider.getRequiredActor();
        if (actor.isApiKey()) {
            return requireApiKeyScope(workspaceId, actor, ApiKeyScope.LINKS_READ);
        }
        return requireUser(workspaceId, LINK_READERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireLinkWriter(UUID workspaceId) {
        AuthenticatedActor actor = actorProvider.getRequiredActor();
        if (actor.isApiKey()) {
            return requireApiKeyScope(workspaceId, actor, ApiKeyScope.LINKS_WRITE);
        }
        return requireUser(workspaceId, LINK_WRITERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireAnalyticsReader(UUID workspaceId) {
        AuthenticatedActor actor = actorProvider.getRequiredActor();
        if (actor.isApiKey()) {
            return requireApiKeyScope(workspaceId, actor, ApiKeyScope.ANALYTICS_READ);
        }
        return requireUser(workspaceId, ANALYTICS_READERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireMemberManager(UUID workspaceId) {
        return requireUser(workspaceId, MEMBER_MANAGERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireCampaignReader(UUID workspaceId) {
        return requireLinkReader(workspaceId);
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireCampaignWriter(UUID workspaceId) {
        return requireUser(workspaceId, LINK_WRITERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireCampaignDeleter(UUID workspaceId) {
        return requireUser(workspaceId, MEMBER_MANAGERS, "Workspace not found");
    }

    @Transactional(readOnly = true)
    public WorkspaceContext requireOwner(UUID workspaceId) {
        return requireUser(workspaceId, EnumSet.of(WorkspaceRole.OWNER), "Workspace not found");
    }

    private WorkspaceContext requireUser(UUID workspaceId, EnumSet<WorkspaceRole> allowedRoles, String notFoundMessage) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        WorkspaceMembershipEntity membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
        if (!allowedRoles.contains(membership.getRole())) {
            throw new ForbiddenException("Workspace role does not permit this operation");
        }
        return new WorkspaceContext(workspaceId, actor.userId(), AuditActorType.USER, membership.getRole(), membership.getWorkspace());
    }

    private WorkspaceContext requireApiKeyScope(UUID workspaceId, AuthenticatedActor actor, ApiKeyScope scope) {
        if (!workspaceId.equals(actor.workspaceId())) {
            throw new ResourceNotFoundException("Workspace not found");
        }
        if (!actor.hasScope(scope)) {
            throw new ForbiddenException("API key scope does not permit this operation");
        }
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return new WorkspaceContext(workspaceId, actor.actorId(), AuditActorType.API_KEY, null, workspace);
    }
}
