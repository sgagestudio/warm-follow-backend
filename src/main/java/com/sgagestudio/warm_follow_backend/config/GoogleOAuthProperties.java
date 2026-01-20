package com.sgagestudio.warm_follow_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.google")
public class GoogleOAuthProperties {
    private boolean enabled;
    private String mode;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;
    private String mockEmail;
    private String mockSub;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getMockEmail() {
        return mockEmail;
    }

    public void setMockEmail(String mockEmail) {
        this.mockEmail = mockEmail;
    }

    public String getMockSub() {
        return mockSub;
    }

    public void setMockSub(String mockSub) {
        this.mockSub = mockSub;
    }
}
