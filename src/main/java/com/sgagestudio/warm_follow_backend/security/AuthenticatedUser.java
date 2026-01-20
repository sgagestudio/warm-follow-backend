package com.sgagestudio.warm_follow_backend.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String provider,
        String authType
) {
}
