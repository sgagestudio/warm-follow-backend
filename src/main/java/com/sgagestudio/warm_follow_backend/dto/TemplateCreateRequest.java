package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TemplateCreateRequest(
        @NotBlank String name,
        String subject,
        @NotBlank String content,
        @NotNull Channel channel,
        Map<String, Object> variables
) {
}
