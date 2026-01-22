package com.sgagestudio.warm_follow_backend.dto;

public record OnboardingStepsResponse(
        boolean workspace_created,
        boolean domain_configured,
        boolean template_created,
        boolean first_reminder_scheduled
) {
}
