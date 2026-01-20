package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TemplatePreviewRequest(
        @NotBlank String content,
        Map<String, Object> data
) {
}
