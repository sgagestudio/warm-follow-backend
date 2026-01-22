package com.sgagestudio.warm_follow_backend.dto;

public record UsageResponse(
        String period,
        int emails_sent,
        int sms_sent,
        int sms_credits_balance,
        int overage_cost_cents
) {
}
