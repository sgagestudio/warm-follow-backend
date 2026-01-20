package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import jakarta.validation.constraints.Email;
import java.util.List;

public record CustomerUpdateRequest(
        String first_name,
        String last_name,
        @Email String email,
        String phone,
        ConsentStatus consent_status,
        List<String> consent_channels,
        String consent_source,
        String consent_proof_ref
) {
}
