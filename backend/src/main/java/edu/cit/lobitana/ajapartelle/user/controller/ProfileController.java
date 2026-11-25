package edu.cit.lobitana.ajapartelle.user.controller;

import edu.cit.lobitana.ajapartelle.auth.security.AuthUser;
import edu.cit.lobitana.ajapartelle.user.dto.UpdateProfileRequest;
import edu.cit.lobitana.ajapartelle.user.dto.UserResponse;
import edu.cit.lobitana.ajapartelle.user.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Own profile for every role (FR-072). The account comes only from the session, never from the request. */
@RestController
@RequestMapping("/api/v1/me/profile")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Profile", description = "View and edit your own profile")
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse get(@AuthenticationPrincipal AuthUser me) {
        return UserResponse.from(userService.getById(me.id()));
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return UserResponse.from(userService.updateProfile(me.id(), req));
    }
}
