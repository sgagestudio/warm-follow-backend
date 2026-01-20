package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookStatusRequest(
        @NotBlank String provider_message_id,
        @NotNull DeliveryStatus status,
        String error_code,
        String error_message
) {
}
