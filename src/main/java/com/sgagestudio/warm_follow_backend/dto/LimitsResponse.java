package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.WorkspacePlan;

public record LimitsResponse(
        WorkspacePlan plan,
        int max_users,
        int max_clients,
        int retention_days,
        Integer sms_included,
        boolean sms_addon_enabled,
        Integer sms_price_cents
) {
}
