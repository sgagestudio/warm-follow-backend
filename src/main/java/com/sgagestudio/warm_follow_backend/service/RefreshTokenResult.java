package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.model.RefreshToken;

public record RefreshTokenResult(
        String rawToken,
        RefreshToken entity
) {
}
