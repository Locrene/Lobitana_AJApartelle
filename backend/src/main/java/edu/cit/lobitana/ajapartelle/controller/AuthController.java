package edu.cit.lobitana.ajapartelle.controller;

import edu.cit.lobitana.ajapartelle.dto.LoginRequest;
import edu.cit.lobitana.ajapartelle.dto.RegisterRequest;
import edu.cit.lobitana.ajapartelle.dto.UserResponse;
import edu.cit.lobitana.ajapartelle.security.AuthCookieService;
import edu.cit.lobitana.ajapartelle.security.AuthUser;
import edu.cit.lobitana.ajapartelle.service.AuthService;
import edu.cit.lobitana.ajapartelle.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService cookies;
    private final edu.cit.lobitana.ajapartelle.security.JwtService jwt;

    public AuthController(UserService userService, AuthService authService,
                          AuthCookieService cookies, edu.cit.lobitana.ajapartelle.security.JwtService jwt) {
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
