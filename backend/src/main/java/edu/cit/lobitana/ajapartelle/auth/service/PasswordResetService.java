package edu.cit.lobitana.ajapartelle.auth.service;

import edu.cit.lobitana.ajapartelle.auth.dto.ResetPasswordRequest;
import edu.cit.lobitana.ajapartelle.auth.entity.PasswordResetToken;
import edu.cit.lobitana.ajapartelle.auth.repository.PasswordResetTokenRepository;
import edu.cit.lobitana.ajapartelle.common.error.ApiException;
import edu.cit.lobitana.ajapartelle.user.entity.User;
import edu.cit.lobitana.ajapartelle.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Creates and redeems single-use password reset tokens (FR-074, FR-075). */
@Service
public class PasswordResetService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int MAX_PER_WINDOW = 3;
    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_LINK = "This reset link is invalid or has expired. Please request a new one.";

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordService passwords;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordService passwords, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.clock = clock;
    }

    /** What the email needs. The raw token exists only here and in the email, so never log this. */
    public record IssuedLink(String email, String fullName, String token, OffsetDateTime expiresAt) {}

    /** Empty when no email may be sent: unknown or deactivated account, or the hourly limit is reached. */
    @Transactional
    public Optional<IssuedLink> issue(String email) {
        User user = users.findIdByEmail(email.trim())
                .flatMap(users::findByIdForUpdate)
                .filter(User::isActive)
                .orElse(null);
        if (user == null) return Optional.empty();

        OffsetDateTime now = now();
        if (tokens.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(RATE_WINDOW)) >= MAX_PER_WINDOW) {
            return Optional.empty();
        }
        tokens.invalidateUnused(user.getId(), now);

        String token = newToken();
        OffsetDateTime expiresAt = now.plus(TOKEN_TTL);
        tokens.save(new PasswordResetToken(user.getId(), hash(token), now, expiresAt));
        return Optional.of(new IssuedLink(user.getEmail(), user.getFullName(), token, expiresAt));
    }

    @Transactional
    public void reset(ResetPasswordRequest req) {
        // Checked first, so that a typo does not use up the link.
        PasswordService.requireMatch(req.newPassword(), req.confirmPassword());
        if (req.token() == null || req.token().isBlank()) throw invalidLink();

        String hash = hash(req.token());
        User user = tokens.findUserIdByHash(hash)
                .flatMap(users::findByIdForUpdate)
                .filter(User::isActive)
                .orElseThrow(PasswordResetService::invalidLink);
        if (tokens.markUsed(hash, now()) == 0) throw invalidLink();
        passwords.replace(user, req.newPassword());
    }

    /** Rows are kept past expiry because the hourly limit counts them. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpiredTokens() {
        tokens.deleteExpiredBefore(now().minusDays(1));
    }

    /** Microseconds, the precision PostgreSQL stores. */
    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static ApiException invalidLink() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", INVALID_LINK);
    }
}
