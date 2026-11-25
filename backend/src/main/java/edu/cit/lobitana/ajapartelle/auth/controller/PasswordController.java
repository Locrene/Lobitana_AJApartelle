package edu.cit.lobitana.ajapartelle.auth.controller;

import edu.cit.lobitana.ajapartelle.auth.dto.*;
import edu.cit.lobitana.ajapartelle.auth.security.AuthUser;
import edu.cit.lobitana.ajapartelle.auth.service.*;
import edu.cit.lobitana.ajapartelle.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Change, forgot and reset password (FR-073, FR-074, FR-075). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Password", description = "Change, forgot and reset password")
public class PasswordController {
    /** The same for every request, so it does not reveal which emails have accounts. */
    private static final String LINK_SENT = "If an account exists, we've sent a link";
    private static final String RESET_DONE = "Your password has been reset. Please sign in with your new password.";

    private final PasswordService passwords;
    private final PasswordResetService resets;
    private final PasswordResetMailer mailer;
    private final AuthCookieService cookies;
    private final JwtService jwt;

    public PasswordController(PasswordService passwords, PasswordResetService resets, PasswordResetMailer mailer,
                              AuthCookieService cookies, JwtService jwt) {
        this.passwords = passwords;
        this.resets = resets;
        this.mailer = mailer;
        this.cookies = cookies;
        this.jwt = jwt;
    }

    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> change(@AuthenticationPrincipal AuthUser me,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        AuthService.LoginResult result = passwords.change(me.id(), req);
        return ResponseEntity.ok()
                .header(AuthCookieService.headerName(), cookies.buildHeader(result.token(), jwt.getTtl()))
                .body(UserResponse.from(result.user()));
    }

    @PostMapping("/auth/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        mailer.requestLink(req.email());
        return new MessageResponse(LINK_SENT);
    }

    @PostMapping("/auth/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) {
        resets.reset(req);
        return new MessageResponse(RESET_DONE);
    }
}
