package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.WorkspaceMembership;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipId;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, WorkspaceMembershipId> {
    List<WorkspaceMembership> findById_UserId(UUID userId);

    Optional<WorkspaceMembership> findById_UserIdAndId_WorkspaceId(UUID userId, UUID workspaceId);

    long countById_WorkspaceIdAndStatus(UUID workspaceId, WorkspaceMembershipStatus status);
}
