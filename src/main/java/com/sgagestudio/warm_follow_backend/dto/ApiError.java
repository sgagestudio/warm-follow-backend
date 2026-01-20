package com.sgagestudio.warm_follow_backend.dto;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, Object> details
) {
}
