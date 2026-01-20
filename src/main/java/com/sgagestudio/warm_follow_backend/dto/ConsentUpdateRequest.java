package com.sgagestudio.warm_follow_backend.dto;

import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConsentUpdateRequest(
        @NotNull ConsentStatus status,
        @NotEmpty List<String> channels,
        @NotBlank String source,
        String proof_ref
) {
}
