package edu.cit.lobitana.ajapartelle.user.config;

import edu.cit.lobitana.ajapartelle.user.repository.UserRepository;
import edu.cit.lobitana.ajapartelle.user.service.UserService;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/** An over-long OWNER_PASSWORD must not crash startup or create an account (FR-043). Runs without Docker. */
class OwnerAccountSeederTest {

    @Test
    void ownerPasswordOver72BytesIsNotSeeded() {
        UserService userService = mock(UserService.class);
        new OwnerAccountSeeder(mock(UserRepository.class), userService,
                "owner@test.local", "a1" + "x".repeat(71), "Test Owner", "09170000000").run(null);
        verifyNoInteractions(userService);
    }
}
