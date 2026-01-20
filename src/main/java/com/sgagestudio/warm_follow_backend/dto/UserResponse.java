package com.sgagestudio.warm_follow_backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String provider,
        String auth_type
) {
}
