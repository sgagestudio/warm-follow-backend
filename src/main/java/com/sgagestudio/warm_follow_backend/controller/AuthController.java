package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.config.AuthProperties;
import com.sgagestudio.warm_follow_backend.dto.AuthResponse;
import com.sgagestudio.warm_follow_backend.dto.LoginRequest;
import com.sgagestudio.warm_follow_backend.dto.LogoutRequest;
import com.sgagestudio.warm_follow_backend.dto.MeResponse;
import com.sgagestudio.warm_follow_backend.dto.OauthAuthorizeResponse;
import com.sgagestudio.warm_follow_backend.dto.OauthExchangeRequest;
import com.sgagestudio.warm_follow_backend.dto.PasswordForgotRequest;
import com.sgagestudio.warm_follow_backend.dto.PasswordResetRequest;
import com.sgagestudio.warm_follow_backend.dto.RefreshRequest;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import com.sgagestudio.warm_follow_backend.dto.StatusResponse;
import com.sgagestudio.warm_follow_backend.service.AuthService;
import com.sgagestudio.warm_follow_backend.service.GoogleOAuthService;
import com.sgagestudio.warm_follow_backend.service.PasswordResetService;
import com.sgagestudio.warm_follow_backend.util.RequestContextHolder;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final PasswordResetService passwordResetService;
    private final AuthProperties authProperties;

    public AuthController(
            AuthService authService,
            GoogleOAuthService googleOAuthService,
            PasswordResetService passwordResetService,
            AuthProperties authProperties
    ) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
        this.passwordResetService = passwordResetService;
        this.authProperties = authProperties;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request, clientIp(), userAgent());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request, clientIp(), userAgent());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refresh_token(), clientIp(), userAgent());
    }

    @PostMapping("/logout")
    public StatusResponse logout(@RequestBody(required = false) LogoutRequest request) {
        String token = request != null ? request.refresh_token() : null;
        authService.logout(java.util.Optional.ofNullable(token));
        return new StatusResponse("ok");
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me();
    }

    @GetMapping("/oauth/google/authorize")
    public OauthAuthorizeResponse authorizeGoogle() {
        return googleOAuthService.buildAuthorizeUrl();
    }

    @PostMapping("/oauth/google/exchange")
    public AuthResponse exchangeGoogle(@Valid @RequestBody OauthExchangeRequest request) {
        return googleOAuthService.exchange(request.code(), request.state(), clientIp(), userAgent());
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, Object>> forgot(@Valid @RequestBody PasswordForgotRequest request) {
        Map<String, Object> response = new HashMap<>();
        passwordResetService.createResetToken(request.email()).ifPresent(token -> {
            if (authProperties.isResetTokenExpose()) {
                response.put("reset_token", token);
            }
        });
        response.putIfAbsent("status", "ok");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/reset")
    public StatusResponse reset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.token(), request.new_password());
        return new StatusResponse("ok");
    }

    private String clientIp() {
        return RequestContextHolder.get().map(ctx -> ctx.ip()).orElse(null);
    }

    private String userAgent() {
        return RequestContextHolder.get().map(ctx -> ctx.userAgent()).orElse(null);
    }
}
