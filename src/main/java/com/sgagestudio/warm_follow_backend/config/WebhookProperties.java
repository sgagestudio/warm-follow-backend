package com.sgagestudio.warm_follow_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook")
public class WebhookProperties {
    private String emailSecret;
    private String smsSecret;

    public String getEmailSecret() {
        return emailSecret;
    }

    public void setEmailSecret(String emailSecret) {
        this.emailSecret = emailSecret;
    }

    public String getSmsSecret() {
        return smsSecret;
    }

    public void setSmsSecret(String smsSecret) {
        this.smsSecret = smsSecret;
    }
}
