package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.JwtProperties;
import com.sgagestudio.warm_follow_backend.dto.AuthResponse;
import com.sgagestudio.warm_follow_backend.dto.LoginRequest;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import com.sgagestudio.warm_follow_backend.dto.UserResponse;
import com.sgagestudio.warm_follow_backend.model.AuthProvider;
import com.sgagestudio.warm_follow_backend.model.AuthType;
import com.sgagestudio.warm_follow_backend.model.RefreshToken;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.AuthenticatedUser;
import com.sgagestudio.warm_follow_backend.security.JwtService;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            AuditService auditService,
            SecurityUtils securityUtils
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    public AuthResponse register(RegisterRequest request, String ip, String userAgent) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (isGmail(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_GOOGLE_REQUIRED", "Use Google OAuth for Gmail accounts");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTH_EMAIL_EXISTS", "Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setProvider(AuthProvider.email);
        user.setAuthType(AuthType.jwt);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        AuthResponse response = issueTokens(user, ip, userAgent);
        auditService.audit("user", user.getId().toString(), "auth.register", null, toUserResponse(user));
        return response;
    }

    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (isGmail(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_GOOGLE_REQUIRED", "Use Google OAuth for Gmail accounts");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials"));
        if (user.getProvider() != AuthProvider.email || user.getAuthType() != AuthType.jwt) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials");
        }
        AuthResponse response = issueTokens(user, ip, userAgent);
        auditService.audit("user", user.getId().toString(), "auth.login", null, toUserResponse(user));
        return response;
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ip, String userAgent) {
        RefreshToken token = refreshTokenService.validate(rawRefreshToken);
        RefreshTokenResult rotated = refreshTokenService.rotate(rawRefreshToken, ip, userAgent);
        User user = token.getUser();
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getProvider().name(),
                user.getAuthType().name()
        );
        String accessToken = jwtService.createAccessToken(principal);
        AuthResponse response = buildAuthResponse(accessToken, rotated.rawToken(), user);
        auditService.audit("user", user.getId().toString(), "auth.refresh", null, toUserResponse(user));
        return response;
    }

    @Transactional
    public void logout(Optional<String> rawRefreshToken) {
        if (rawRefreshToken.isPresent()) {
            String tokenValue = rawRefreshToken.get();
            RefreshToken token = refreshTokenService.findByRawToken(tokenValue);
            refreshTokenService.revoke(tokenValue);
            if (token != null && token.getUser() != null) {
                auditService.audit("user", token.getUser().getId().toString(), "auth.logout", null, null);
            }
            return;
        }
        refreshTokenService.revokeAllForUser(securityUtils.requireCurrentUserId());
        auditService.audit("user", securityUtils.requireCurrentUserId().toString(), "auth.logout", null, null);
    }

    public UserResponse me() {
        User user = userRepository.findById(securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return toUserResponse(user);
    }

    private AuthResponse issueTokens(User user, String ip, String userAgent) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getProvider().name(),
                user.getAuthType().name()
        );
        String accessToken = jwtService.createAccessToken(principal);
        RefreshTokenResult refreshToken = refreshTokenService.create(user, ip, userAgent);
        return buildAuthResponse(accessToken, refreshToken.rawToken(), user);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        Duration ttl = jwtProperties.getAccessTokenTtl();
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                ttl.getSeconds(),
                toUserResponse(user)
        );
    }

    private boolean isGmail(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return false;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        return domain.equals("gmail.com") || domain.equals("googlemail.com");
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProvider().name(),
                user.getAuthType().name()
        );
    }
}
