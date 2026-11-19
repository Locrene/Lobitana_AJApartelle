package edu.cit.lobitana.ajapartelle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Authentication is handled by our own JWT filter + users table, so Spring's default
// in-memory user ("generated security password") is disabled.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class AjApartelleApplication {
    public static void main(String[] args) {
        SpringApplication.run(AjApartelleApplication.class, args);
    }
}
