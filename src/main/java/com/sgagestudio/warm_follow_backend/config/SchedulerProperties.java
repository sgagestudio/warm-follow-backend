package com.sgagestudio.warm_follow_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {
    private int reminderIntervalMinutes = 5;

    public int getReminderIntervalMinutes() {
        return reminderIntervalMinutes;
    }

    public void setReminderIntervalMinutes(int reminderIntervalMinutes) {
        this.reminderIntervalMinutes = reminderIntervalMinutes;
    }
}
