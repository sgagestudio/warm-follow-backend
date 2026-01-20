package com.sgagestudio.warm_follow_backend.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.time")
public class TimeProperties {
    private String zone = "Europe/Madrid";

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public ZoneId getZoneId() {
        return ZoneId.of(zone);
    }
}
