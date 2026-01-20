package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID reminder_id,
        TransactionStatus status,
        String request_id,
        String idempotency_key,
        Instant started_at,
        Instant finished_at,
        Instant created_at,
        Map<String, Long> delivery_counts
) {
}
