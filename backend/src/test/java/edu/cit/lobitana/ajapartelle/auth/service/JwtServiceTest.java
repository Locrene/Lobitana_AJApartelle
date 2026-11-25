package edu.cit.lobitana.ajapartelle.auth.service;

import edu.cit.lobitana.ajapartelle.user.entity.Role;
import edu.cit.lobitana.ajapartelle.user.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the system JWT (FR-042: 8-hour expiry). Run without Docker. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-longer-than-32-bytes";

    private static User user(long id, Role role) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setRole(role);
        return u;
    }

    @Test
    void issuedTokenParsesBackWithSubjectRoleAndEightHourExpiry() {
        JwtService jwt = new JwtService(SECRET, 480);
        Claims claims = jwt.parse(jwt.issue(user(42L, Role.STAFF))).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("STAFF");
        assertThat(claims.getId()).isNotBlank();
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetimeMs).isEqualTo(Duration.ofHours(8).toMillis());
    }

    @Test
    void issuedTokenCarriesTheUsersTokenVersion() {
        JwtService jwt = new JwtService(SECRET, 480);
        User u = user(7L, Role.GUEST);
        u.incrementTokenVersion();

        Claims claims = jwt.parse(jwt.issue(u)).orElseThrow();
        assertThat(claims.get("tv", Integer.class)).isEqualTo(1);
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        JwtService jwt = new JwtService(SECRET, 480);
        String token = jwt.issue(user(1L, Role.GUEST));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        JwtService other = new JwtService("another-secret-that-is-longer-than-32-bytes", 480);

        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse(other.issue(user(1L, Role.OWNER)))).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void shortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 480))
                .isInstanceOf(IllegalStateException.class);
    }
}
