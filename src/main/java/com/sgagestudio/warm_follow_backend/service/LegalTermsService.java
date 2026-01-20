package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sgagestudio.warm_follow_backend.model.UserSettings;
import com.sgagestudio.warm_follow_backend.repository.UserSettingsRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LegalTermsService {
    private static final String KEY_ACCEPTED = "legal_terms_accepted";

    private final UserSettingsRepository userSettingsRepository;

    public LegalTermsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void requireAccepted(UUID userId) {
        if (!isAccepted(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "LEGAL_TERMS_REQUIRED", "Legal terms acceptance required");
        }
    }

    public boolean isAccepted(UUID userId) {
        UserSettings settings = userSettingsRepository.findById(userId).orElse(null);
        if (settings == null) {
            return false;
        }
        JsonNode node = settings.getSettings();
        if (node == null) {
            return false;
        }
        return node.path(KEY_ACCEPTED).asBoolean(false);
    }
}
