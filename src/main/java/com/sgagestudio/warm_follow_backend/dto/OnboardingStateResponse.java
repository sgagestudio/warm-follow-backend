package com.sgagestudio.warm_follow_backend.dto;

public record OnboardingStateResponse(
        OnboardingStepsResponse steps,
        OnboardingStep current_step
) {
}
