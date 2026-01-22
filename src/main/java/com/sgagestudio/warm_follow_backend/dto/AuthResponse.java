package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;

public record AuthResponse(
        String access_token,
        String refresh_token,
        String token_type,
        long expires_in,
        UserResponse user,
        WorkspaceResponse workspace,
        WorkspaceRole role
) {
}
