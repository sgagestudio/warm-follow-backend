package com.sgagestudio.warm_follow_backend.security;

import com.sgagestudio.warm_follow_backend.config.SecurityProperties;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecurityUtils {
    private final SecurityProperties securityProperties;

    public SecurityUtils(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public UUID requireCurrentUserId() {
        return getAuthenticatedUser().userId();
    }

    public String requireCurrentEmail() {
        return getAuthenticatedUser().email();
    }

    public AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return user;
    }

    public AuthenticatedUser getAuthenticatedUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public boolean isAdmin() {
        String email = requireCurrentEmail();
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return securityProperties.getAdminEmails().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(email.toLowerCase(Locale.ROOT)));
    }
}
