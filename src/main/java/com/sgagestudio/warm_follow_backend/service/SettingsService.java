package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.SettingsResponse;
import com.sgagestudio.warm_follow_backend.dto.SettingsUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.UserSettings;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.repository.UserSettingsRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public SettingsService(
            UserSettingsRepository userSettingsRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            AuditService auditService,
            SecurityUtils securityUtils
    ) {
        this.userSettingsRepository = userSettingsRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    public SettingsResponse getSettings() {
        UUID userId = securityUtils.requireCurrentUserId();
        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> createDefaults(userId));
        return toResponse(settings);
    }

    public SettingsResponse update(SettingsUpdateRequest request) {
        UUID userId = securityUtils.requireCurrentUserId();
        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> createDefaults(userId));
        ObjectNode node = settings.getSettings() instanceof ObjectNode obj
                ? obj
                : objectMapper.createObjectNode();
        Map<String, Object> updates = request.settings();
        if (updates != null) {
            updates.forEach((key, value) -> node.set(key, objectMapper.valueToTree(value)));
        }
        settings.setSettings(node);
        UserSettings saved = userSettingsRepository.save(settings);
        auditService.audit(
                "user_settings",
                userId.toString(),
                "settings.update",
                null,
                Map.of("updated_at", saved.getUpdatedAt())
        );
        return toResponse(saved);
    }

    private UserSettings createDefaults(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        settings.setSettings(objectMapper.createObjectNode());
        return userSettingsRepository.save(settings);
    }

    private SettingsResponse toResponse(UserSettings settings) {
        return new SettingsResponse(
                settings.getUserId(),
                objectMapper.convertValue(settings.getSettings(), Map.class),
                settings.getUpdatedAt()
        );
    }
}
