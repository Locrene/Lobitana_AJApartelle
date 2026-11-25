package edu.cit.lobitana.ajapartelle.common.security;

import edu.cit.lobitana.ajapartelle.common.validation.Utf8;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * bcrypt reads only the first 72 bytes of a password. The Spring Security fix for CVE-2025-22228 makes
 * encode() refuse longer passwords, but since 6.3.9 (CVE-2025-22234) matches() still truncates them, which
 * would accept a correct 72-byte password followed by anything. This encoder refuses those too.
 */
public class StrictBCryptPasswordEncoder extends BCryptPasswordEncoder {
    public static final int MAX_BYTES = 72;

    public StrictBCryptPasswordEncoder(int strength) {
        super(strength);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // Always hash first, so that an over-long guess takes as long as any other wrong password.
        boolean matches = super.matches(rawPassword, encodedPassword);
        return matches && Utf8.byteLength(rawPassword) <= MAX_BYTES;
    }
}
