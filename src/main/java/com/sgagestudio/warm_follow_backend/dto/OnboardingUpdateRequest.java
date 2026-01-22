package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotNull;

public record OnboardingUpdateRequest(
        @NotNull OnboardingStep step,
        boolean completed
) {
}
