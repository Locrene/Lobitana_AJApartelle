package edu.cit.lobitana.ajapartelle.common.security;

import edu.cit.lobitana.ajapartelle.common.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The application's password encoder and bcrypt's 72-byte limit (FR-071, CVE-2025-22228).
 * Spring's own fix limits encode() only, so matches() is checked here too. Runs without Docker.
 */
class PasswordEncoderTest {
    private static final String BYTES_72 = "a1" + "x".repeat(70);
    private static final String MULTIBYTE_72 = "a1" + "\u00e9".repeat(35);

    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void wrongSuffixIsNotAccepted() {
        String hash = encoder.encode(BYTES_72);
        assertThat(encoder.matches(BYTES_72, hash)).isTrue();
        assertThat(encoder.matches(BYTES_72 + "WRONG", hash)).isFalse();
    }

    @Test
    void theLimitCountsBytesNotCharacters() {
        String hash = encoder.encode(MULTIBYTE_72);
        assertThat(encoder.matches(MULTIBYTE_72, hash)).isTrue();
        assertThat(encoder.matches(MULTIBYTE_72 + "\u00e9", hash)).isFalse();
    }

    @Test
    void over72BytesCannotBeHashed() {
        assertThatThrownBy(() -> encoder.encode(BYTES_72 + "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
