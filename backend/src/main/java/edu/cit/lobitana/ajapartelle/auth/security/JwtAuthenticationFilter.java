package edu.cit.lobitana.ajapartelle.auth.security;

import edu.cit.lobitana.ajapartelle.auth.repository.RevokedTokenRepository;
import edu.cit.lobitana.ajapartelle.auth.service.AuthCookieService;
import edu.cit.lobitana.ajapartelle.auth.service.JwtService;
import edu.cit.lobitana.ajapartelle.user.entity.User;
import edu.cit.lobitana.ajapartelle.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the JWT from the httpOnly cookie, verifies signature/expiry, rejects revoked tokens
 * (server-side logout), deactivated accounts (SRS FR-045) and outdated token versions (FR-073, FR-075),
 * and loads the CURRENT role from the database.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthCookieService cookies;
    private final UserRepository users;
    private final RevokedTokenRepository revoked;

    public JwtAuthenticationFilter(JwtService jwtService, AuthCookieService cookies,
                                   UserRepository users, RevokedTokenRepository revoked) {
        this.jwtService = jwtService;
        this.cookies = cookies;
        this.users = users;
        this.revoked = revoked;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        cookies.read(request).flatMap(jwtService::parse).ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        if (claims.getId() == null || revoked.existsById(claims.getId())) return;
        Long userId;
        try { userId = Long.valueOf(claims.getSubject()); } catch (NumberFormatException e) { return; }
        User user = users.findById(userId).orElse(null);
        if (user == null || !user.isActive()) return;
        Integer version = claims.get(JwtService.TOKEN_VERSION_CLAIM, Integer.class);
        if (version == null || version != user.getTokenVersion()) return;

        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole(),
                claims.getId(), claims.getExpiration().toInstant());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
