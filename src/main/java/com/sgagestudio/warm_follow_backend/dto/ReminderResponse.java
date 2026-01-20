package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        Long template_id,
        Channel channel,
        ReminderFrequency frequency,
        LocalTime scheduled_time,
        LocalDate scheduled_date,
        Instant next_run,
        ReminderStatus status,
        long recipients_count,
        Instant created_at,
        Instant updated_at
) {
}
