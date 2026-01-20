package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;

public record TemplateUpdateRequest(
        String name,
        String subject,
        String content,
        Channel channel
) {
}
