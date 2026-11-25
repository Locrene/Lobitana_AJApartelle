package edu.cit.lobitana.ajapartelle.auth.service;

import edu.cit.lobitana.ajapartelle.auth.dto.ChangePasswordRequest;
import edu.cit.lobitana.ajapartelle.auth.repository.PasswordResetTokenRepository;
import edu.cit.lobitana.ajapartelle.common.error.ApiException;
import edu.cit.lobitana.ajapartelle.user.entity.User;
import edu.cit.lobitana.ajapartelle.user.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Password rules that need more than annotations, and changing a password (FR-073). */
@Service
public class PasswordService {
    private final UserService userService;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;

    public PasswordService(UserService userService, PasswordResetTokenRepository resetTokens,
                           PasswordEncoder encoder, JwtService jwt, Clock clock) {
        this.userService = userService;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
    }

    /** Changes the password, ends every session and returns a token for the session that made the change. */
    @Transactional
    public AuthService.LoginResult change(Long userId, ChangePasswordRequest req) {
        requireMatch(req.newPassword(), req.confirmPassword());
        User user = userService.getByIdForUpdate(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.invalidField("currentPassword", "Current password is incorrect");
        }
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw ApiException.invalidField("newPassword", "New password must be different from the current password");
        }
        replace(user, req.newPassword());
        return new AuthService.LoginResult(user, jwt.issue(user));
    }

    static void requireMatch(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw ApiException.invalidField("confirmPassword", "Passwords do not match");
        }
    }

    /** The one place a password is replaced. The caller holds the lock on the user row. */
    void replace(User user, String rawPassword) {
        user.setPasswordHash(encoder.encode(rawPassword));
        user.incrementTokenVersion();
        resetTokens.invalidateUnused(user.getId(), OffsetDateTime.now(clock));
    }
}
