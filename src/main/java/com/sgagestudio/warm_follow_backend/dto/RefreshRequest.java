package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refresh_token
) {
}
