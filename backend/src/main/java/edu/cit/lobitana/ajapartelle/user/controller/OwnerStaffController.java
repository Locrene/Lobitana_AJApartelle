package edu.cit.lobitana.ajapartelle.user.controller;

import edu.cit.lobitana.ajapartelle.user.dto.CreateStaffRequest;
import edu.cit.lobitana.ajapartelle.user.dto.UpdateStatusRequest;
import edu.cit.lobitana.ajapartelle.user.dto.UserResponse;
import edu.cit.lobitana.ajapartelle.user.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Owner management of staff accounts (SRS FR-061, FR-062, Appendix E: GET/POST/PATCH /owner/staff). */
@RestController
@RequestMapping("/api/v1/owner/staff")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "User", description = "Owner management of staff accounts")
public class OwnerStaffController {
    private final UserService userService;

    public OwnerStaffController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listStaff().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.createStaff(req)));
    }

    @PatchMapping("/{id}")
    public UserResponse setActive(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return UserResponse.from(userService.setStaffActive(id, req.active()));
    }
}
