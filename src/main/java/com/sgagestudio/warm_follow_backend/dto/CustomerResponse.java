package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String first_name,
        String last_name,
        String email,
        String phone,
        ConsentStatus consent_status,
        LocalDate consent_date,
        String consent_source,
        List<String> consent_channels,
        String consent_proof_ref,
        boolean is_erased,
        Instant created_at,
        Instant updated_at
) {
}
