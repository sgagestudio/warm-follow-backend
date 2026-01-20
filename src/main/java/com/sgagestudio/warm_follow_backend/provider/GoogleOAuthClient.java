package com.sgagestudio.warm_follow_backend.provider;

public interface GoogleOAuthClient {
    GoogleProfile exchangeCode(String code, String codeVerifier);

    record GoogleProfile(String email, String subject) {
    }
}
