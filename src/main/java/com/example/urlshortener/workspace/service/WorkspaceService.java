package com.example.urlshortener.workspace.service;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ForbiddenException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.AddMemberRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.CreateWorkspaceRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.MemberResponse;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.UpdateMemberRoleRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.WorkspaceResponse;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceMembershipEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final WorkspaceAuthorizationService authorizationService;
    private final AuditService auditService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMembershipRepository membershipRepository,
                            UserRepository userRepository,
                            CurrentOwnerProvider currentOwnerProvider,
                            WorkspaceAuthorizationService authorizationService,
                            AuditService auditService) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.currentOwnerProvider = currentOwnerProvider;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public WorkspaceEntity createDefaultWorkspace(UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.save(new WorkspaceEntity(UUID.randomUUID(), defaultWorkspaceName(user), true, user));
        membershipRepository.save(new WorkspaceMembershipEntity(workspace, user, WorkspaceRole.OWNER, true));
        auditService.recordUser(
                AuditAction.WORKSPACE_CREATED,
                workspace.getId(),
                user.getId(),
                AuditResourceType.WORKSPACE,
                workspace.getId(),
                auditService.workspaceCreatedMetadata(true)
        );
        return workspace;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> list() {
        UUID actorUserId = currentOwnerProvider.getCurrentOwner().userId();
        return membershipRepository.findByUserIdWithWorkspace(actorUserId).stream()
                .map(this::toWorkspaceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID workspaceId) {
        WorkspaceContext context = authorizationService.requireLinkReader(workspaceId);
        return new WorkspaceResponse(context.workspaceId(), context.workspace().getName(), context.role(), context.workspace().isDefaultWorkspace());
    }

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        UserEntity owner = userRepository.getReferenceById(currentOwnerProvider.getCurrentOwner().userId());
        WorkspaceEntity workspace = workspaceRepository.save(new WorkspaceEntity(UUID.randomUUID(), request.name().trim(), false, owner));
        WorkspaceMembershipEntity membership = membershipRepository.save(new WorkspaceMembershipEntity(workspace, owner, WorkspaceRole.OWNER, false));
        auditService.recordUser(
                AuditAction.WORKSPACE_CREATED,
                workspace.getId(),
                owner.getId(),
                AuditResourceType.WORKSPACE,
                workspace.getId(),
                auditService.workspaceCreatedMetadata(false)
        );
        return toWorkspaceResponse(membership);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID workspaceId) {
        authorizationService.requireMemberManager(workspaceId);
        return membershipRepository.findByWorkspaceIdWithUser(workspaceId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID workspaceId, AddMemberRequest request) {
        WorkspaceContext actor = authorizationService.requireMemberManager(workspaceId);
        validateRoleChange(actor.role(), null, request.role());
        WorkspaceEntity workspace = actor.workspace();
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new BadRequestException("User is already a workspace member");
        }
        WorkspaceMembershipEntity membership = membershipRepository.save(new WorkspaceMembershipEntity(workspace, user, request.role(), false));
        auditService.recordUser(
                AuditAction.WORKSPACE_MEMBER_ADDED,
                workspaceId,
                actor.actorUserId(),
                AuditResourceType.WORKSPACE_MEMBER,
                user.getId(),
                auditService.memberRoleMetadata(null, request.role().name(), false)
        );
        return toMemberResponse(membership);
    }

    @Transactional
    public MemberResponse updateMember(UUID workspaceId, UUID userId, UpdateMemberRoleRequest request) {
        WorkspaceContext actor = authorizationService.requireMemberManager(workspaceId);
        WorkspaceMembershipEntity membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));
        validateRoleChange(actor.role(), membership.getRole(), request.role());
        if (membership.getRole() == WorkspaceRole.OWNER && request.role() != WorkspaceRole.OWNER) {
            ensureAnotherOwner(workspaceId);
        }
        WorkspaceRole previousRole = membership.getRole();
        membership.setRole(request.role());
        MemberResponse response = toMemberResponse(membershipRepository.save(membership));
        auditService.recordUser(
                AuditAction.WORKSPACE_MEMBER_ROLE_CHANGED,
                workspaceId,
                actor.actorUserId(),
                AuditResourceType.WORKSPACE_MEMBER,
                userId,
                auditService.memberRoleMetadata(previousRole.name(), request.role().name(), membership.isDefaultMembership())
        );
        return response;
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID userId) {
        WorkspaceContext actor = authorizationService.requireMemberManager(workspaceId);
        WorkspaceMembershipEntity membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));
        validateRoleChange(actor.role(), membership.getRole(), null);
        if (membership.getRole() == WorkspaceRole.OWNER) {
            ensureAnotherOwner(workspaceId);
        }
        WorkspaceRole previousRole = membership.getRole();
        boolean defaultMembership = membership.isDefaultMembership();
        membershipRepository.delete(membership);
        auditService.recordUser(
                AuditAction.WORKSPACE_MEMBER_REMOVED,
                workspaceId,
                actor.actorUserId(),
                AuditResourceType.WORKSPACE_MEMBER,
                userId,
                auditService.memberRoleMetadata(previousRole.name(), null, defaultMembership)
        );
    }

    private void validateRoleChange(WorkspaceRole actorRole, WorkspaceRole oldRole, WorkspaceRole newRole) {
        if (actorRole == WorkspaceRole.OWNER) {
            return;
        }
        if (newRole == WorkspaceRole.OWNER || oldRole == WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only workspace owners can manage owner membership");
        }
    }

    private void ensureAnotherOwner(UUID workspaceId) {
        if (membershipRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER) <= 1) {
            throw new BadRequestException("Workspace must retain at least one owner");
        }
    }

    private WorkspaceResponse toWorkspaceResponse(WorkspaceMembershipEntity membership) {
        WorkspaceEntity workspace = membership.getWorkspace();
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), membership.getRole(), workspace.isDefaultWorkspace());
    }

    private MemberResponse toMemberResponse(WorkspaceMembershipEntity membership) {
        return new MemberResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.isDefaultMembership()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultWorkspaceName(UserEntity user) {
        String localPart = user.getEmail().split("@", 2)[0];
        return localPart + "'s Workspace";
    }
}
