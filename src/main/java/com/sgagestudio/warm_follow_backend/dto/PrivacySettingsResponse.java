package com.sgagestudio.warm_follow_backend.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivacySettingsResponse(
        UUID user_id,
        boolean data_retention_enabled,
        boolean anonymization_enabled,
        boolean audit_logs_enabled,
        boolean encryption_enabled,
        Integer retention_days,
        Instant updated_at
) {
}
