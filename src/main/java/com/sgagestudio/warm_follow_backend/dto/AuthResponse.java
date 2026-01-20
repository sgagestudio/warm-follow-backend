package com.sgagestudio.warm_follow_backend.dto;

public record AuthResponse(
        String access_token,
        String refresh_token,
        String token_type,
        long expires_in,
        UserResponse user
) {
}
