package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.WorkspacePlan;
import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import com.sgagestudio.warm_follow_backend.model.WorkspaceStatus;
import java.util.UUID;

public record WorkspaceContextResponse(
        UUID id,
        String name,
        WorkspacePlan plan,
        WorkspaceStatus status,
        WorkspaceRole role
) {
}
