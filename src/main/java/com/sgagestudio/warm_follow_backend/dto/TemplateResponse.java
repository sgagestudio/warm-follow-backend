package com.sgagestudio.warm_follow_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sgagestudio.warm_follow_backend.model.Channel;
import java.time.Instant;

public record TemplateResponse(
        Long id,
        String name,
        String subject,
        String content,
        Channel channel,
        JsonNode variables,
        int version,
        Instant created_at,
        Instant updated_at
) {
}
