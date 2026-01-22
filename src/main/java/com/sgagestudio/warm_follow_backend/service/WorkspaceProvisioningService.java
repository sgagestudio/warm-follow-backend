package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.model.Workspace;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembership;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipId;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipStatus;
import com.sgagestudio.warm_follow_backend.model.WorkspacePlan;
import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import com.sgagestudio.warm_follow_backend.model.WorkspaceStatus;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceMembershipRepository;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkspaceProvisioningService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceProvisioningService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    public WorkspaceMembership createOwnerWorkspace(User user, String name) {
        Workspace workspace = new Workspace();
        workspace.setName(resolveWorkspaceName(user, name));
        workspace.setPlan(WorkspacePlan.starter);
        workspace.setStatus(WorkspaceStatus.active);
        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setId(new WorkspaceMembershipId(saved.getId(), user.getId()));
        membership.setRole(WorkspaceRole.owner);
        membership.setStatus(WorkspaceMembershipStatus.active);
        return membershipRepository.save(membership);
    }

    public WorkspaceMembership ensureDefaultWorkspace(User user, String name) {
        List<WorkspaceMembership> memberships = membershipRepository.findById_UserId(user.getId());
        if (memberships.isEmpty()) {
            return createOwnerWorkspace(user, name);
        }
        return memberships.stream()
                .filter(member -> member.getStatus() == WorkspaceMembershipStatus.active)
                .sorted(Comparator.comparingInt(member -> rolePriority(member.getRole())))
                .findFirst()
                .orElse(memberships.get(0));
    }

    private String resolveWorkspaceName(User user, String name) {
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        String base = StringUtils.hasText(user.getName()) ? user.getName().trim() : "Workspace";
        return base + " Workspace";
    }

    private int rolePriority(WorkspaceRole role) {
        return switch (role) {
            case owner -> 0;
            case admin -> 1;
            case member -> 2;
        };
    }
}
