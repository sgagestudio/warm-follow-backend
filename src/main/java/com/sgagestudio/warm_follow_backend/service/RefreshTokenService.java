package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.config.JwtProperties;
import com.sgagestudio.warm_follow_backend.model.RefreshToken;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.repository.RefreshTokenRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.TokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public RefreshTokenResult create(User user, String ip, String userAgent) {
        String rawToken = generateToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256(rawToken));
        token.setIssuedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
        token.setIp(ip);
        token.setUserAgent(userAgent);
        refreshTokenRepository.save(token);
        return new RefreshTokenResult(rawToken, token);
    }

    public RefreshToken validate(String rawToken) {
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID", "Invalid refresh token"));
        if (token.getRevokedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_REVOKED", "Refresh token revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_EXPIRED", "Refresh token expired");
        }
        return token;
    }

    public RefreshTokenResult rotate(String rawToken, String ip, String userAgent) {
        RefreshToken token = validate(rawToken);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        return create(token.getUser(), ip, userAgent);
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = TokenHasher.sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(userId);
        if (tokens.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        tokens.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(tokens);
    }

    public RefreshToken findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String hash = TokenHasher.sha256(rawToken);
        return refreshTokenRepository.findByTokenHash(hash).orElse(null);
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
