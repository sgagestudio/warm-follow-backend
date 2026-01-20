package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record ComplianceAssessmentRequest(
        @NotEmpty Map<String, Object> details
) {
}
