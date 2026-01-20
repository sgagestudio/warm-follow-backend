package com.sgagestudio.warm_follow_backend.dto;

public record ErrorResponse(
        ApiError error,
        String request_id
) {
}
