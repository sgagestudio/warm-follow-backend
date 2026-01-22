package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SendingDomainCreateRequest(
        @NotBlank String domain
) {
}
