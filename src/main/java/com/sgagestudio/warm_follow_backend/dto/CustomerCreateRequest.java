package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CustomerCreateRequest(
        @NotBlank String first_name,
        @NotBlank String last_name,
        @Email @NotBlank String email,
        String phone,
        @NotNull ConsentStatus consent_status,
        @NotEmpty List<String> consent_channels,
        @NotBlank String consent_source,
        String consent_proof_ref
) {
}
