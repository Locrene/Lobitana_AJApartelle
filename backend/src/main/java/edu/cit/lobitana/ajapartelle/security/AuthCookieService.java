package edu.cit.lobitana.ajapartelle.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** Stores the JWT in an httpOnly cookie so page scripts can never read it. */
@Component
public class AuthCookieService {
    private final String name;
    private final boolean secure;

    public AuthCookieService(@Value("${app.cookie.name}") String name,
                             @Value("${app.cookie.secure}") boolean secure) {
        this.name = name;
        this.secure = secure;
    }

    public String buildHeader(String token, Duration maxAge) {
        return ResponseCookie.from(name, token)
                .httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(maxAge)
                .build().toString();
    }

    public String clearHeader() {
        return buildHeader("", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    public static String headerName() { return HttpHeaders.SET_COOKIE; }
}
