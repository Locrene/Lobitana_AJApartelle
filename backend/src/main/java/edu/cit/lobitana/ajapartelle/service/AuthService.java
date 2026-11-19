package edu.cit.lobitana.ajapartelle.service;

import edu.cit.lobitana.ajapartelle.dto.LoginRequest;
import edu.cit.lobitana.ajapartelle.entity.RevokedToken;
import edu.cit.lobitana.ajapartelle.entity.User;
import edu.cit.lobitana.ajapartelle.exception.ApiException;
import edu.cit.lobitana.ajapartelle.repository.RevokedTokenRepository;
import edu.cit.lobitana.ajapartelle.repository.UserRepository;
import edu.cit.lobitana.ajapartelle.security.AuthUser;
import edu.cit.lobitana.ajapartelle.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AuthService {
    /** Same message for unknown email and wrong password, so attackers cannot tell which was wrong. */
    private static final String INVALID = "Invalid email or password.";

    private final UserRepository users;
    private final RevokedTokenRepository revoked;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    /** Hash compared against when the email does not exist, so response time does not reveal it either. */
    private final String dummyHash;

    public AuthService(UserRepository users, RevokedTokenRepository revoked, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.revoked = revoked;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("timing-equalizer-not-a-real-password");
    }

    public record LoginResult(User user, String token) {}

    public LoginResult login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID);
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED",
                    "This account has been deactivated. Please contact the owner.");
        }
        return new LoginResult(user, jwt.issue(user));
    }

    /** Server-side logout: the token id is blacklisted until the token would have expired anyway. */
    public void logout(AuthUser principal) {
        if (principal == null) return;
        revoked.save(new RevokedToken(principal.tokenId(),
                OffsetDateTime.ofInstant(principal.tokenExpiresAt(), ZoneOffset.UTC)));
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpiredRevocations() {
        revoked.deleteExpired(OffsetDateTime.now());
    }
}
