package com.sgagestudio.warm_follow_backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private Duration resetTokenTtl;
    private boolean resetTokenExpose;

    public Duration getResetTokenTtl() {
        return resetTokenTtl;
    }

    public void setResetTokenTtl(Duration resetTokenTtl) {
        this.resetTokenTtl = resetTokenTtl;
    }

    public boolean isResetTokenExpose() {
        return resetTokenExpose;
    }

    public void setResetTokenExpose(boolean resetTokenExpose) {
        this.resetTokenExpose = resetTokenExpose;
    }
}
