package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReminderUpdateRequest(
        Long template_id,
        Channel channel,
        ReminderFrequency frequency,
        LocalTime scheduled_time,
        LocalDate scheduled_date
) {
}
