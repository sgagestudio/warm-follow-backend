package com.sgagestudio.warm_follow_backend.dto;

import java.util.Map;

public record SettingsUpdateRequest(
        Map<String, Object> settings
) {
}
