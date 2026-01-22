package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.dto.LimitsResponse;
import com.sgagestudio.warm_follow_backend.model.WorkspacePlan;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitsService {

    public LimitsResponse resolveLimits(WorkspacePlan plan) {
        return switch (plan) {
            case starter -> new LimitsResponse(plan, 1, 200, 30, 0, true, null);
            case pro -> new LimitsResponse(plan, 3, 1000, 180, 10, true, null);
            case business -> new LimitsResponse(plan, 10, 5000, 365, 50, true, null);
        };
    }
}
