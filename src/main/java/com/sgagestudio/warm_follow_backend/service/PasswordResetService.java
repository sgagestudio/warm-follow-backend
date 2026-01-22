package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.AuthProperties;
import com.sgagestudio.warm_follow_backend.dto.UserResponse;
import com.sgagestudio.warm_follow_backend.model.AuthType;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembership;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.TokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {
    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final WorkspaceContextService workspaceContextService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, ResetTokenEntry> tokens = new ConcurrentHashMap<>();

    public PasswordResetService(
            AuthProperties authProperties,
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            WorkspaceContextService workspaceContextService
    ) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.workspaceContextService = workspaceContextService;
    }

    public Optional<String> createResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (user.getAuthType() != AuthType.jwt) {
            return Optional.empty();
        }
        String rawToken = generateToken();
        String hash = TokenHasher.sha256(rawToken);
        tokens.put(hash, new ResetTokenEntry(user.getId(), Instant.now().plus(authProperties.getResetTokenTtl())));
        WorkspaceMembership membership = workspaceContextService.resolveDefaultMembership(user.getId());
        auditService.audit(membership.getWorkspaceId(), "user", user.getId().toString(), "auth.password_forgot", null, null);
        return Optional.of(rawToken);
    }

    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHasher.sha256(rawToken);
        ResetTokenEntry entry = tokens.get(hash);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_RESET_INVALID", "Invalid or expired reset token");
        }
        User user = userRepository.findById(entry.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getAuthType() != AuthType.jwt) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_RESET_NOT_ALLOWED", "Password reset not allowed");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        tokens.remove(hash);
        WorkspaceMembership membership = workspaceContextService.resolveDefaultMembership(user.getId());
        auditService.audit(membership.getWorkspaceId(), "user", user.getId().toString(), "auth.password_reset", null, new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus().name(),
                user.getLastLogin()
        ));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record ResetTokenEntry(UUID userId, Instant expiresAt) {
    }
}
