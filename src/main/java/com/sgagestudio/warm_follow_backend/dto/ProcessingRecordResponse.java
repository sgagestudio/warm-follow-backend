package com.sgagestudio.warm_follow_backend.dto;

import java.time.Instant;
import java.util.Map;

public record ProcessingRecordResponse(
        Instant generated_at,
        Map<String, Long> actions,
        Map<String, Long> entities
) {
}
