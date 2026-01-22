package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.model.Workspace;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembership;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipStatus;
import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceMembershipRepository;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceRepository;
import com.sgagestudio.warm_follow_backend.security.AuthenticatedUser;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.RequestContextHolder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkspaceContextService {
    private final WorkspaceMembershipRepository membershipRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SecurityUtils securityUtils;

    public WorkspaceContextService(
            WorkspaceMembershipRepository membershipRepository,
            WorkspaceRepository workspaceRepository,
            SecurityUtils securityUtils
    ) {
        this.membershipRepository = membershipRepository;
        this.workspaceRepository = workspaceRepository;
        this.securityUtils = securityUtils;
    }

    public WorkspaceContext requireContext() {
        AuthenticatedUser user = securityUtils.getAuthenticatedUser();
        UUID headerWorkspaceId = parseWorkspaceHeader();
        List<WorkspaceMembership> memberships = membershipRepository.findById_UserId(user.userId());
        if (memberships.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_ACCESS_DENIED", "No workspace access");
        }

        WorkspaceMembership selected = null;
        if (headerWorkspaceId != null) {
            selected = memberships.stream()
                    .filter(member -> headerWorkspaceId.equals(member.getWorkspaceId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_FORBIDDEN", "Workspace access denied"));
        } else if (user.workspaceId() != null) {
            UUID tokenWorkspaceId = user.workspaceId();
            selected = memberships.stream()
                    .filter(member -> tokenWorkspaceId.equals(member.getWorkspaceId()))
                    .findFirst()
                    .orElse(null);
        }

        if (selected == null) {
            if (memberships.size() == 1) {
                selected = memberships.get(0);
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "WORKSPACE_REQUIRED", "Workspace selection required");
            }
        }

        if (selected.getStatus() != WorkspaceMembershipStatus.active) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_INACTIVE", "Workspace membership inactive");
        }

        Workspace workspace = workspaceRepository.findById(selected.getWorkspaceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found"));
        return new WorkspaceContext(workspace, selected);
    }

    public WorkspaceMembership resolveDefaultMembership(UUID userId) {
        List<WorkspaceMembership> memberships = membershipRepository.findById_UserId(userId);
        if (memberships.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_ACCESS_DENIED", "No workspace access");
        }
        return memberships.stream()
                .filter(member -> member.getStatus() == WorkspaceMembershipStatus.active)
                .sorted(Comparator.comparingInt(member -> rolePriority(member.getRole())))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_INACTIVE", "Workspace membership inactive"));
    }

    public List<WorkspaceContext> listContexts(UUID userId) {
        List<WorkspaceMembership> memberships = membershipRepository.findById_UserId(userId);
        List<UUID> workspaceIds = memberships.stream().map(WorkspaceMembership::getWorkspaceId).distinct().toList();
        Map<UUID, Workspace> workspaceMap = workspaceRepository.findAllById(workspaceIds).stream()
                .collect(java.util.stream.Collectors.toMap(Workspace::getId, workspace -> workspace));
        return memberships.stream()
                .map(member -> new WorkspaceContext(workspaceMap.get(member.getWorkspaceId()), member))
                .filter(ctx -> ctx.workspace() != null)
                .toList();
    }

    private UUID parseWorkspaceHeader() {
        String header = RequestContextHolder.get().map(ctx -> ctx.workspaceId()).orElse(null);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORKSPACE_ID_INVALID", "Invalid workspace id");
        }
    }

    private int rolePriority(WorkspaceRole role) {
        return switch (role) {
            case owner -> 0;
            case admin -> 1;
            case member -> 2;
        };
    }

    public record WorkspaceContext(Workspace workspace, WorkspaceMembership membership) {
    }
}
