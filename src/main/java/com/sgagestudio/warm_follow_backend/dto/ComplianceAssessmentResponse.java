package com.sgagestudio.warm_follow_backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ComplianceAssessmentResponse(
        UUID id,
        Instant created_at
) {
}
