package com.sgagestudio.warm_follow_backend.dto;

public record DashboardStatsResponse(
        long total_customers,
        long active_reminders,
        long sent_this_week,
        double delivery_rate,
        long consented_customers
) {
}
