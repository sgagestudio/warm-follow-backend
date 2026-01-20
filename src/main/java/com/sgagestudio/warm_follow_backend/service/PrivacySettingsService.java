package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.PrivacySettingsResponse;
import com.sgagestudio.warm_follow_backend.dto.PrivacySettingsUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.PrivacySettings;
import com.sgagestudio.warm_follow_backend.repository.PrivacySettingsRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PrivacySettingsService {
    private final PrivacySettingsRepository privacySettingsRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public PrivacySettingsService(
            PrivacySettingsRepository privacySettingsRepository,
            UserRepository userRepository,
            AuditService auditService,
            SecurityUtils securityUtils
    ) {
        this.privacySettingsRepository = privacySettingsRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    public PrivacySettingsResponse getSettings() {
        UUID userId = securityUtils.requireCurrentUserId();
        PrivacySettings settings = privacySettingsRepository.findById(userId)
                .orElseGet(() -> createDefaults(userId));
        return toResponse(settings);
    }

    public PrivacySettingsResponse update(PrivacySettingsUpdateRequest request) {
        UUID userId = securityUtils.requireCurrentUserId();
        PrivacySettings settings = privacySettingsRepository.findById(userId)
                .orElseGet(() -> createDefaults(userId));
        if (request.data_retention_enabled() != null) {
            settings.setDataRetentionEnabled(request.data_retention_enabled());
        }
        if (request.anonymization_enabled() != null) {
            settings.setAnonymizationEnabled(request.anonymization_enabled());
        }
        if (request.audit_logs_enabled() != null) {
            settings.setAuditLogsEnabled(request.audit_logs_enabled());
        }
        if (request.encryption_enabled() != null) {
            settings.setEncryptionEnabled(request.encryption_enabled());
        }
        if (request.retention_days() != null) {
            settings.setRetentionDays(request.retention_days());
        }
        PrivacySettings saved = privacySettingsRepository.save(settings);
        auditService.audit(
                "privacy_settings",
                userId.toString(),
                "privacy.update",
                null,
                java.util.Map.of("updated_at", saved.getUpdatedAt())
        );
        return toResponse(saved);
    }

    private PrivacySettings createDefaults(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        PrivacySettings settings = new PrivacySettings();
        settings.setUserId(userId);
        return privacySettingsRepository.save(settings);
    }

    private PrivacySettingsResponse toResponse(PrivacySettings settings) {
        return new PrivacySettingsResponse(
                settings.getUserId(),
                settings.isDataRetentionEnabled(),
                settings.isAnonymizationEnabled(),
                settings.isAuditLogsEnabled(),
                settings.isEncryptionEnabled(),
                settings.getRetentionDays(),
                settings.getUpdatedAt()
        );
    }
}
