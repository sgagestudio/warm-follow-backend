package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ReminderCreateRequest(
        @NotNull Long template_id,
        @NotEmpty List<UUID> customer_ids,
        @NotNull Channel channel,
        @NotNull ReminderFrequency frequency,
        @NotNull LocalTime scheduled_time,
        LocalDate scheduled_date
) {
}
