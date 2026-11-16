package com.cadence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Global timezone configuration anchoring all date/time calculations.
 */
@Configuration
public class TimezoneConfig {

    @Value("${cadence.timezone:Asia/Kolkata}")
    private String configuredTimezone;

    @Bean
    public ZoneId defaultZoneId() {
        ZoneId zoneId = ZoneId.of(configuredTimezone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        return zoneId;
    }
}
