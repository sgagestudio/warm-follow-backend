package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.GdprRequestStatus;
import java.util.UUID;

public record GdprRequestResponse(
        UUID id,
        GdprRequestStatus status
) {
}
