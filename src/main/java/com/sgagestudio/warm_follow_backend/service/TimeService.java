package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.config.TimeProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class TimeService {
    private final ZoneId zoneId;

    public TimeService(TimeProperties properties) {
        this.zoneId = properties.getZoneId();
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public LocalDate today() {
        return LocalDate.now(zoneId);
    }

    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zoneId).toInstant();
    }

    public Instant startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    public Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, zoneId).toInstant();
    }
}
