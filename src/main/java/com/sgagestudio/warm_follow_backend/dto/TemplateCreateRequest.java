package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateCreateRequest(
        @NotBlank String name,
        String subject,
        @NotBlank String content,
        @NotNull Channel channel
) {
}
