package com.sgagestudio.warm_follow_backend.dto;

public record PrivacySettingsUpdateRequest(
        Boolean data_retention_enabled,
        Boolean anonymization_enabled,
        Boolean audit_logs_enabled,
        Boolean encryption_enabled,
        Integer retention_days
) {
}
