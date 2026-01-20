package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsentEventResponse(
        UUID id,
        ConsentStatus status,
        List<String> channels,
        String proof_ref,
        String source,
        Instant created_at
) {
}
