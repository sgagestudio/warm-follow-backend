package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sgagestudio.warm_follow_backend.config.EmailProperties;
import com.sgagestudio.warm_follow_backend.model.UserSettings;
import com.sgagestudio.warm_follow_backend.repository.UserSettingsRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailIdentityService {
    private static final String KEY_COMPANY_NAME = "companyName";
    private final UserSettingsRepository userSettingsRepository;
    private final EmailProperties emailProperties;
    private final MailProperties mailProperties;

    public EmailIdentityService(
            UserSettingsRepository userSettingsRepository,
            EmailProperties emailProperties,
            MailProperties mailProperties
    ) {
        this.userSettingsRepository = userSettingsRepository;
        this.emailProperties = emailProperties;
        this.mailProperties = mailProperties;
    }

    public EmailIdentity resolve(UUID ownerUserId) {
        String companyName = loadCompanyName(ownerUserId);
        String localPart = toLocalPart(companyName);
        String fromAddress = null;
        if (StringUtils.hasText(localPart)) {
            fromAddress = localPart + "@" + emailProperties.getDomain() + ".com";
        }
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = emailProperties.getFromAddress();
        }
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = mailProperties.getUsername();
        }
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("Missing email from address. Set app.email.from-address or spring.mail.username.");
        }
        String fromName = StringUtils.hasText(companyName) ? companyName : emailProperties.getFromName();
        return new EmailIdentity(fromAddress, fromName);
    }

    private String loadCompanyName(UUID ownerUserId) {
        UserSettings settings = userSettingsRepository.findById(ownerUserId).orElse(null);
        if (settings == null) {
            return null;
        }
        JsonNode node = settings.getSettings();
        if (node == null || !node.has(KEY_COMPANY_NAME)) {
            return null;
        }
        JsonNode value = node.get(KEY_COMPANY_NAME);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private String toLocalPart(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        String normalized = companyName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        String sanitized = normalized.replaceAll("[^a-z0-9._-]", "");
        return StringUtils.hasText(sanitized) ? sanitized : null;
    }

    public record EmailIdentity(String address, String name) {
    }
}
