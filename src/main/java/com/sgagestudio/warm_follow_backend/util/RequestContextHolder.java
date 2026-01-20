package com.sgagestudio.warm_follow_backend.util;

import java.util.Optional;
import java.util.UUID;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<RequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static String getRequestId() {
        return get().map(RequestContext::requestId).orElseGet(RequestContextHolder::generateRequestId);
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
