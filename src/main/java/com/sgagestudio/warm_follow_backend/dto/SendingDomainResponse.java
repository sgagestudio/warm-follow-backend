package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.SendingDomainStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SendingDomainResponse(
        UUID id,
        String domain,
        SendingDomainStatus status,
        List<DnsRecord> verification_records,
        Instant created_at,
        Instant updated_at
) {
}
