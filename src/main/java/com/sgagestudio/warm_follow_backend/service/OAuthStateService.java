package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OAuthStateEntry> states = new ConcurrentHashMap<>();

    public OAuthState createState() {
        String state = generateToken(24);
        String verifier = generateToken(64);
        String challenge = codeChallenge(verifier);
        states.put(state, new OAuthStateEntry(verifier, Instant.now().plus(10, ChronoUnit.MINUTES)));
        return new OAuthState(state, challenge);
    }

    public String consumeVerifier(String state) {
        OAuthStateEntry entry = states.remove(state);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_STATE_INVALID", "Invalid OAuth state");
        }
        return entry.codeVerifier();
    }

    private String generateToken(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String codeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private record OAuthStateEntry(String codeVerifier, Instant expiresAt) {
    }

    public record OAuthState(String state, String codeChallenge) {
    }
}
