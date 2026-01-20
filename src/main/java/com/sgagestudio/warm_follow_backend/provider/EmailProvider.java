package com.sgagestudio.warm_follow_backend.provider;

import java.util.Map;

public interface EmailProvider {
    String sendEmail(EmailRequest request);

    record EmailRequest(
            String to,
            String subject,
            String body,
            Map<String, Object> metadata,
            String fromAddress,
            String fromName
    ) {
    }
}
