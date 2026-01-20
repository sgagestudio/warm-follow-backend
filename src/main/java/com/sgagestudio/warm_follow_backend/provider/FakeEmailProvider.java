package com.sgagestudio.warm_follow_backend.provider;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "fake")
public class FakeEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(FakeEmailProvider.class);

    @Override
    public String sendEmail(EmailRequest request) {
        String messageId = UUID.randomUUID().toString();
        log.info(
                "FakeEmailProvider sendEmail to={} subject={} from={} messageId={}",
                request.to(),
                request.subject(),
                request.fromAddress(),
                messageId
        );
        return messageId;
    }
}
