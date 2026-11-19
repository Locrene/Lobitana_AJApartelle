package edu.cit.lobitana.ajapartelle.config;

import edu.cit.lobitana.ajapartelle.entity.Role;
import edu.cit.lobitana.ajapartelle.repository.UserRepository;
import edu.cit.lobitana.ajapartelle.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first OWNER account when the system is installed (SRS FR-043).
 * Credentials come only from the environment (.env); the password is stored as a bcrypt hash.
 */
@Component
public class OwnerAccountSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OwnerAccountSeeder.class);

    private final UserRepository users;
    private final UserService userService;
    private final String email, password, fullName, mobile;

    public OwnerAccountSeeder(UserRepository users, UserService userService,
                              @Value("${app.owner.email}") String email,
                              @Value("${app.owner.password}") String password,
                              @Value("${app.owner.full-name}") String fullName,
                              @Value("${app.owner.mobile}") String mobile) {
        this.users = users;
        this.userService = userService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.mobile = mobile;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.OWNER)) return;
        if (email.isBlank() || password.isBlank()) {
            log.warn("No OWNER account exists. Set OWNER_EMAIL and OWNER_PASSWORD in backend/.env and restart.");
            return;
        }
        if (password.length() < 8) {
            log.warn("OWNER_PASSWORD must be at least 8 characters; owner account not created.");
            return;
        }
        userService.create(fullName, email, mobile, password, Role.OWNER);
        log.info("Initial OWNER account created for {}", email);
    }
}
