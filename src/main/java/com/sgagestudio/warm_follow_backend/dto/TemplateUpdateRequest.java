package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import java.util.Map;

public record TemplateUpdateRequest(
        String name,
        String subject,
        String content,
        Channel channel,
        Map<String, Object> variables
) {
}
