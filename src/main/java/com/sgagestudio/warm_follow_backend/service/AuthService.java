package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.JwtProperties;
import com.sgagestudio.warm_follow_backend.dto.AuthResponse;
import com.sgagestudio.warm_follow_backend.dto.CountsResponse;
import com.sgagestudio.warm_follow_backend.dto.LoginRequest;
import com.sgagestudio.warm_follow_backend.dto.MeResponse;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import com.sgagestudio.warm_follow_backend.dto.UserResponse;
import com.sgagestudio.warm_follow_backend.dto.WorkspaceContextResponse;
import com.sgagestudio.warm_follow_backend.dto.WorkspaceResponse;
import com.sgagestudio.warm_follow_backend.model.AuthProvider;
import com.sgagestudio.warm_follow_backend.model.AuthType;
import com.sgagestudio.warm_follow_backend.model.RefreshToken;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.model.Workspace;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembership;
import com.sgagestudio.warm_follow_backend.model.WorkspaceMembershipStatus;
import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import com.sgagestudio.warm_follow_backend.repository.CustomerRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceMembershipRepository;
import com.sgagestudio.warm_follow_backend.repository.WorkspaceRepository;
import com.sgagestudio.warm_follow_backend.security.AuthenticatedUser;
import com.sgagestudio.warm_follow_backend.security.JwtService;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private final WorkspaceProvisioningService workspaceProvisioningService;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceRepository workspaceRepository;
    private final UsageService usageService;
    private final PlanLimitsService planLimitsService;
    private final CustomerRepository customerRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            AuditService auditService,
            SecurityUtils securityUtils,
            WorkspaceProvisioningService workspaceProvisioningService,
            WorkspaceContextService workspaceContextService,
            WorkspaceRepository workspaceRepository,
            UsageService usageService,
            PlanLimitsService planLimitsService,
            CustomerRepository customerRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.workspaceProvisioningService = workspaceProvisioningService;
        this.workspaceContextService = workspaceContextService;
        this.workspaceRepository = workspaceRepository;
        this.usageService = usageService;
        this.planLimitsService = planLimitsService;
        this.customerRepository = customerRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
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

        WorkspaceMembership membership = workspaceProvisioningService.createOwnerWorkspace(user, request.workspace_name());
        AuthResponse response = issueTokens(user, membership, ip, userAgent);
        auditService.audit(membership.getWorkspaceId(), "user", user.getId().toString(), "auth.register", null, toUserResponse(user));
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
        user.setLastLogin(Instant.now());
        userRepository.save(user);
        WorkspaceMembership membership = workspaceContextService.resolveDefaultMembership(user.getId());
        AuthResponse response = issueTokens(user, membership, ip, userAgent);
        auditService.audit(membership.getWorkspaceId(), "user", user.getId().toString(), "auth.login", null, toUserResponse(user));
        return response;
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ip, String userAgent) {
        RefreshToken token = refreshTokenService.validate(rawRefreshToken);
        RefreshTokenResult rotated = refreshTokenService.rotate(rawRefreshToken, ip, userAgent);
        User user = token.getUser();
        WorkspaceMembership membership = workspaceContextService.resolveDefaultMembership(user.getId());
        AuthenticatedUser principal = buildPrincipal(user, membership);
        String accessToken = jwtService.createAccessToken(principal);
        Workspace workspace = requireWorkspace(membership.getWorkspaceId());
        AuthResponse response = buildAuthResponse(accessToken, rotated.rawToken(), user, workspace, membership.getRole());
        auditService.audit(membership.getWorkspaceId(), "user", user.getId().toString(), "auth.refresh", null, toUserResponse(user));
        return response;
    }

    @Transactional
    public void logout(Optional<String> rawRefreshToken) {
        if (rawRefreshToken.isPresent()) {
            String tokenValue = rawRefreshToken.get();
            RefreshToken token = refreshTokenService.findByRawToken(tokenValue);
            refreshTokenService.revoke(tokenValue);
            if (token != null && token.getUser() != null) {
                WorkspaceMembership membership = workspaceContextService.resolveDefaultMembership(token.getUser().getId());
                auditService.audit(membership.getWorkspaceId(), "user", token.getUser().getId().toString(), "auth.logout", null, null);
            }
            return;
        }
        refreshTokenService.revokeAllForUser(securityUtils.requireCurrentUserId());
        auditService.audit("user", securityUtils.requireCurrentUserId().toString(), "auth.logout", null, null);
    }

    public MeResponse me() {
        User user = userRepository.findById(securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        WorkspaceContextService.WorkspaceContext context = workspaceContextService.requireContext();
        Workspace workspace = context.workspace();
        WorkspaceRole role = context.membership().getRole();
        var usage = usageService.getCurrentUsage(workspace.getId());
        var limits = planLimitsService.resolveLimits(workspace.getPlan());
        var counts = new CountsResponse(
                customerRepository.countByWorkspaceIdAndErasedFalse(workspace.getId()),
                workspaceMembershipRepository.countById_WorkspaceIdAndStatus(workspace.getId(), WorkspaceMembershipStatus.active)
        );
        var contexts = workspaceContextService.listContexts(user.getId()).stream()
                .map(item -> new WorkspaceContextResponse(
                        item.workspace().getId(),
                        item.workspace().getName(),
                        item.workspace().getPlan(),
                        item.workspace().getStatus(),
                        item.membership().getRole()
                ))
                .toList();
        return new MeResponse(
                toUserResponse(user),
                toWorkspaceResponse(workspace),
                role,
                limits,
                usage,
                counts,
                contexts.size() > 1 ? contexts : null
        );
    }

    private AuthResponse issueTokens(User user, WorkspaceMembership membership, String ip, String userAgent) {
        AuthenticatedUser principal = buildPrincipal(user, membership);
        String accessToken = jwtService.createAccessToken(principal);
        RefreshTokenResult refreshToken = refreshTokenService.create(user, ip, userAgent);
        Workspace workspace = requireWorkspace(membership.getWorkspaceId());
        return buildAuthResponse(accessToken, refreshToken.rawToken(), user, workspace, membership.getRole());
    }

    private AuthResponse buildAuthResponse(
            String accessToken,
            String refreshToken,
            User user,
            Workspace workspace,
            WorkspaceRole role
    ) {
        Duration ttl = jwtProperties.getAccessTokenTtl();
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                ttl.getSeconds(),
                toUserResponse(user),
                toWorkspaceResponse(workspace),
                role
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
                user.getStatus().name(),
                user.getLastLogin()
        );
    }

    private WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getPlan(),
                workspace.getStatus(),
                null,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private Workspace requireWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found"));
    }

    private AuthenticatedUser buildPrincipal(User user, WorkspaceMembership membership) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getProvider().name(),
                user.getAuthType().name(),
                membership.getWorkspaceId(),
                membership.getRole().name()
        );
    }
}
