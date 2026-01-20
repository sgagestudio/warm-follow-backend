package com.sgagestudio.warm_follow_backend.util;

public record RequestContext(
        String requestId,
        String ip,
        String userAgent,
        String idempotencyKey
) {
}
