package com.sgagestudio.warm_follow_backend.provider;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FakeSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(FakeSmsProvider.class);

    @Override
    public String sendSms(SmsRequest request) {
        String messageId = UUID.randomUUID().toString();
        log.info("FakeSmsProvider sendSms to={} messageId={}", request.to(), messageId);
        return messageId;
    }
}
