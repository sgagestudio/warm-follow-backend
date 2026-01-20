package com.sgagestudio.warm_follow_backend.provider;

import java.util.Map;

public interface SmsProvider {
    String sendSms(SmsRequest request);

    record SmsRequest(String to, String body, Map<String, Object> metadata) {
    }
}
