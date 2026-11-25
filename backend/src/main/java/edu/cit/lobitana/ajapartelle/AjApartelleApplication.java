package edu.cit.lobitana.ajapartelle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

// Authentication is handled by our own JWT filter + users table, so Spring's default
// in-memory user ("generated security password") is disabled.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class AjApartelleApplication {
    /** Business time zone for dates and schedules (FR-003, FR-055). Matches app.timezone. */
    public static final String BUSINESS_TIME_ZONE = "Asia/Manila";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_TIME_ZONE));
        SpringApplication.run(AjApartelleApplication.class, args);
    }
}
