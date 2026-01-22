package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.WorkspacePlan;
import com.sgagestudio.warm_follow_backend.model.WorkspaceStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        WorkspacePlan plan,
        WorkspaceStatus status,
        String billing_status,
        Instant created_at,
        Instant updated_at
) {
}
