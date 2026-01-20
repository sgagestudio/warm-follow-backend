package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.DeliveryChannel;
import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID transaction_id,
        UUID customer_id,
        DeliveryChannel channel,
        DeliveryStatus status,
        String provider_message_id,
        String error_code,
        String error_message,
        Instant created_at,
        Instant updated_at
) {
}
