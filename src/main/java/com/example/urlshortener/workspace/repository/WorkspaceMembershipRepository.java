package com.example.urlshortener.workspace.repository;

import com.example.urlshortener.workspace.entity.WorkspaceMembershipEntity;
import com.example.urlshortener.workspace.entity.WorkspaceMembershipId;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembershipEntity, WorkspaceMembershipId> {
    @Query("select membership from WorkspaceMembershipEntity membership join fetch membership.workspace where membership.user.id = :userId order by membership.defaultMembership desc, membership.workspace.createdAt asc")
    List<WorkspaceMembershipEntity> findByUserIdWithWorkspace(@Param("userId") UUID userId);

    @Query("select membership from WorkspaceMembershipEntity membership join fetch membership.user where membership.workspace.id = :workspaceId order by membership.role asc, membership.user.email asc")
    List<WorkspaceMembershipEntity> findByWorkspaceIdWithUser(@Param("workspaceId") UUID workspaceId);

    @Query("select membership from WorkspaceMembershipEntity membership join fetch membership.workspace where membership.workspace.id = :workspaceId and membership.user.id = :userId")
    Optional<WorkspaceMembershipEntity> findByWorkspaceIdAndUserId(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    @Query("select count(membership) > 0 from WorkspaceMembershipEntity membership where membership.workspace.id = :workspaceId and membership.user.id = :userId")
    boolean existsByWorkspaceIdAndUserId(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    @Query("select count(membership) from WorkspaceMembershipEntity membership where membership.workspace.id = :workspaceId and membership.role = :role")
    long countByWorkspaceIdAndRole(@Param("workspaceId") UUID workspaceId, @Param("role") WorkspaceRole role);
}
