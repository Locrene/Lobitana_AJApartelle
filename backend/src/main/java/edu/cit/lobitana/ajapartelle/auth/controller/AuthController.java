package edu.cit.lobitana.ajapartelle.auth.controller;

import edu.cit.lobitana.ajapartelle.auth.dto.LoginRequest;
import edu.cit.lobitana.ajapartelle.auth.security.AuthUser;
import edu.cit.lobitana.ajapartelle.auth.service.AuthCookieService;
import edu.cit.lobitana.ajapartelle.auth.service.AuthService;
import edu.cit.lobitana.ajapartelle.auth.service.JwtService;
import edu.cit.lobitana.ajapartelle.user.dto.RegisterRequest;
import edu.cit.lobitana.ajapartelle.user.dto.UserResponse;
import edu.cit.lobitana.ajapartelle.user.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Registration, sign-in and sign-out (FR-071, FR-042, FR-045, FR-046). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Register, sign in, sign out and current user")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService cookies;
    private final JwtService jwt;

    public AuthController(UserService userService, AuthService authService,
                          AuthCookieService cookies, JwtService jwt) {
        this.userService = userService;
        this.authService = authService;
        this.cookies = cookies;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.registerGuest(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.LoginResult result = authService.login(req);
        return ResponseEntity.ok()
                .header(AuthCookieService.headerName(), cookies.buildHeader(result.token(), jwt.getTtl()))
                .body(UserResponse.from(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthUser principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().header(AuthCookieService.headerName(), cookies.clearHeader()).build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser principal) {
        return UserResponse.from(userService.getById(principal.id()));
    }
}
