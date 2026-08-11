package com.example.urlshortener.testsupport;

import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceMembershipEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;

import java.util.UUID;

public final class WorkspaceTestSupport {
    private WorkspaceTestSupport() {
    }

    public static WorkspaceEntity defaultWorkspace(UserEntity user,
                                                   WorkspaceRepository workspaceRepository,
                                                   WorkspaceMembershipRepository membershipRepository) {
        return membershipRepository.findByUserIdWithWorkspace(user.getId()).stream()
                .filter(WorkspaceMembershipEntity::isDefaultMembership)
                .findFirst()
                .map(WorkspaceMembershipEntity::getWorkspace)
                .orElseGet(() -> createDefaultWorkspace(user, workspaceRepository, membershipRepository));
    }

    private static WorkspaceEntity createDefaultWorkspace(UserEntity user,
                                                          WorkspaceRepository workspaceRepository,
                                                          WorkspaceMembershipRepository membershipRepository) {
        WorkspaceEntity workspace = workspaceRepository.saveAndFlush(
                new WorkspaceEntity(UUID.randomUUID(), "Test Workspace", true, user));
        membershipRepository.saveAndFlush(
                new WorkspaceMembershipEntity(workspace, user, WorkspaceRole.OWNER, true));
        return workspace;
    }

    public static WorkspaceEntity workspace(UserEntity user,
                                            WorkspaceRole role,
                                            WorkspaceRepository workspaceRepository,
                                            WorkspaceMembershipRepository membershipRepository) {
        WorkspaceEntity workspace = workspaceRepository.saveAndFlush(
                new WorkspaceEntity(UUID.randomUUID(), "Shared Workspace", false, user));
        membershipRepository.saveAndFlush(
                new WorkspaceMembershipEntity(workspace, user, role, false));
        return workspace;
    }

    public static void member(WorkspaceEntity workspace,
                              UserEntity user,
                              WorkspaceRole role,
                              WorkspaceMembershipRepository membershipRepository) {
        membershipRepository.saveAndFlush(new WorkspaceMembershipEntity(workspace, user, role, false));
    }
}
