package edu.cit.lobitana.ajapartelle.service;

import edu.cit.lobitana.ajapartelle.dto.CreateStaffRequest;
import edu.cit.lobitana.ajapartelle.dto.RegisterRequest;
import edu.cit.lobitana.ajapartelle.entity.Role;
import edu.cit.lobitana.ajapartelle.entity.User;
import edu.cit.lobitana.ajapartelle.exception.ApiException;
import edu.cit.lobitana.ajapartelle.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Public registration always creates a GUEST (SRS FR-043). */
    @Transactional
    public User registerGuest(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match.");
        }
        return create(req.fullName(), req.email(), req.mobileNumber(), req.password(), Role.GUEST);
    }

    /** Owner adds a staff account (SRS FR-061). */
    @Transactional
    public User createStaff(CreateStaffRequest req) {
        return create(req.fullName(), req.email(), req.mobileNumber(), req.password(), Role.STAFF);
    }

    @Transactional
    public User create(String fullName, String email, String mobile, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists.");
        }
        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(normalizedEmail);
        u.setMobileNumber(mobile == null || mobile.isBlank() ? null : mobile.trim());
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        u.setActive(true);
        return users.saveAndFlush(u);
    }

    public List<User> listStaff() {
        return users.findByRoleOrderByFullNameAsc(Role.STAFF);
    }

    /** Activate / deactivate a STAFF account. Owners cannot deactivate themselves (FR-062). */
    @Transactional
    public User setStaffActive(Long staffId, boolean active) {
        User u = users.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Staff account not found."));
        if (u.getRole() != Role.STAFF) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_STAFF", "Only staff accounts can be activated or deactivated here.");
        }
        u.setActive(active);
        return users.save(u);
    }

    public User getById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please sign in to continue."));
    }
}
