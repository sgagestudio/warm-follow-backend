package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.GoogleOAuthProperties;
import com.sgagestudio.warm_follow_backend.config.JwtProperties;
import com.sgagestudio.warm_follow_backend.dto.AuthResponse;
import com.sgagestudio.warm_follow_backend.dto.OauthAuthorizeResponse;
import com.sgagestudio.warm_follow_backend.model.AuthProvider;
import com.sgagestudio.warm_follow_backend.model.AuthType;
import com.sgagestudio.warm_follow_backend.model.OauthIdentity;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.provider.GoogleOAuthClient;
import com.sgagestudio.warm_follow_backend.repository.OauthIdentityRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.AuthenticatedUser;
import com.sgagestudio.warm_follow_backend.security.JwtService;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleOAuthService {
    private final GoogleOAuthProperties properties;
    private final OAuthStateService stateService;
    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final OauthIdentityRepository oauthIdentityRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            OAuthStateService stateService,
            GoogleOAuthClient googleOAuthClient,
            UserRepository userRepository,
            OauthIdentityRepository oauthIdentityRepository,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            AuditService auditService
    ) {
        this.properties = properties;
        this.stateService = stateService;
        this.googleOAuthClient = googleOAuthClient;
        this.userRepository = userRepository;
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    public OauthAuthorizeResponse buildAuthorizeUrl() {
        OAuthStateService.OAuthState state = stateService.createState();
        String url = UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScopes())
                .queryParam("state", state.state())
                .queryParam("code_challenge", state.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
        return new OauthAuthorizeResponse(url);
    }

    @Transactional
    public AuthResponse exchange(String code, String state, String ip, String userAgent) {
        String verifier = stateService.consumeVerifier(state);
        GoogleOAuthClient.GoogleProfile profile = googleOAuthClient.exchangeCode(code, verifier);
        String email = profile.email().trim().toLowerCase(Locale.ROOT);

        User user = oauthIdentityRepository.findByProviderAndProviderSubject(AuthProvider.google, profile.subject())
                .map(OauthIdentity::getUser)
                .orElseGet(() -> upsertUserFromProfile(email, profile.subject()));

        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getProvider().name(),
                user.getAuthType().name()
        );
        String accessToken = jwtService.createAccessToken(principal);
        RefreshTokenResult refreshToken = refreshTokenService.create(user, ip, userAgent);
        Duration ttl = jwtProperties.getAccessTokenTtl();
        com.sgagestudio.warm_follow_backend.dto.UserResponse userResponse = new com.sgagestudio.warm_follow_backend.dto.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProvider().name(),
                user.getAuthType().name()
        );
        AuthResponse response = new AuthResponse(
                accessToken,
                refreshToken.rawToken(),
                "Bearer",
                ttl.getSeconds(),
                userResponse
        );
        auditService.audit("user", user.getId().toString(), "auth.oauth_login", null, userResponse);
        return response;
    }

    private User upsertUserFromProfile(String email, String subject) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        User user = existing.orElseGet(User::new);
        user.setEmail(email);
        if (!StringUtils.hasText(user.getName())) {
            String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            user.setName(name);
        }
        user.setProvider(AuthProvider.google);
        user.setAuthType(AuthType.oauth);
        user.setPasswordHash(null);
        userRepository.save(user);

        oauthIdentityRepository.findByUser_IdAndProvider(user.getId(), AuthProvider.google)
                .orElseGet(() -> {
                    OauthIdentity identity = new OauthIdentity();
                    identity.setUser(user);
                    identity.setProvider(AuthProvider.google);
                    identity.setProviderSubject(subject);
                    identity.setEmail(email);
                    return oauthIdentityRepository.save(identity);
                });
        return user;
    }
}
