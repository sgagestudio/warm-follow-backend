package com.sgagestudio.warm_follow_backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actor_user_id,
        String entity_type,
        String entity_id,
        String action,
        String request_id,
        Instant created_at
) {
}
