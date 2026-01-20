package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record OauthExchangeRequest(
        @NotBlank String code,
        @NotBlank String state
) {
}
