package com.sgagestudio.warm_follow_backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SettingsResponse(
        UUID user_id,
        Map<String, Object> settings,
        Instant updated_at
) {
}
