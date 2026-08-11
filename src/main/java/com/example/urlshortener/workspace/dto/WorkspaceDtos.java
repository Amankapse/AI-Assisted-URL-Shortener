package com.example.urlshortener.workspace.dto;

import com.example.urlshortener.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class WorkspaceDtos {
    private WorkspaceDtos() {
    }

    public record CreateWorkspaceRequest(
            @NotBlank @Size(max = 160) String name
    ) {
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            WorkspaceRole role,
            boolean defaultWorkspace
    ) {
    }

    public record AddMemberRequest(
            @NotBlank @Email String email,
            @NotNull WorkspaceRole role
    ) {
    }

    public record UpdateMemberRoleRequest(
            @NotNull WorkspaceRole role
    ) {
    }

    public record MemberResponse(
            UUID userId,
            String email,
            WorkspaceRole role,
            boolean defaultMembership
    ) {
    }
}
