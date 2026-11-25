package edu.cit.lobitana.ajapartelle.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Business time zone (SRS FR-003, FR-055): business dates and schedules use Asia/Manila.
 * Timestamps are stored as TIMESTAMPTZ (absolute instants); this zone is used to decide
 * "today", check-in dates and the 9:00 AM reminder schedule.
 */
@Configuration
public class TimeZoneConfig {

    @Bean
    public ZoneId businessZone(@Value("${app.timezone}") String zone) {
        return ZoneId.of(zone);
    }

    @Bean
    public Clock businessClock(ZoneId businessZone) {
        return Clock.system(businessZone);
    }
}
