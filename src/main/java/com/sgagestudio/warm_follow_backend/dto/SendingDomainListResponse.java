package com.sgagestudio.warm_follow_backend.dto;

import java.util.List;

public record SendingDomainListResponse(
        List<SendingDomainResponse> items
) {
}
