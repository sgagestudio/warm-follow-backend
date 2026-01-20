package com.sgagestudio.warm_follow_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordForgotRequest(
        @Email @NotBlank String email
) {
}
