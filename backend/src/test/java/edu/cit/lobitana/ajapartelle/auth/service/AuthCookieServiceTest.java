package edu.cit.lobitana.ajapartelle.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** NFR-07 (web): the JWT cookie is httpOnly and SameSite=Strict, and Secure when COOKIE_SECURE=true. */
class AuthCookieServiceTest {

    @Test
    void cookieIsHttpOnlyAndStrictButNotSecureOverPlainHttp() {
        String header = new AuthCookieService("AJ_TOKEN", false).buildHeader("abc", Duration.ofHours(8));
        assertThat(header).startsWith("AJ_TOKEN=abc").contains("HttpOnly").contains("SameSite=Strict")
                .contains("Path=/").contains("Max-Age=28800").doesNotContain("Secure");
    }

    @Test
    void cookieIsSecureWhenServedOverHttps() {
        String header = new AuthCookieService("AJ_TOKEN", true).buildHeader("abc", Duration.ofHours(8));
        assertThat(header).contains("Secure").contains("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    void signOutCookieExpiresImmediately() {
        String header = new AuthCookieService("AJ_TOKEN", false).clearHeader();
        assertThat(header).startsWith("AJ_TOKEN=;").contains("Max-Age=0").contains("HttpOnly");
    }
}
