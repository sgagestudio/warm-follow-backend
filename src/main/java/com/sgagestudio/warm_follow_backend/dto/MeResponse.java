package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import java.util.List;

public record MeResponse(
        UserResponse user,
        WorkspaceResponse workspace,
        WorkspaceRole role,
        LimitsResponse limits,
        UsageResponse usage,
        CountsResponse counts,
        List<WorkspaceContextResponse> workspaces
) {
}
