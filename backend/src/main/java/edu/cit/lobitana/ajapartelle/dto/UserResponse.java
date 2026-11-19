package edu.cit.lobitana.ajapartelle.dto;

import edu.cit.lobitana.ajapartelle.entity.User;
import java.time.OffsetDateTime;

/** Public view of a user. Never includes the password hash. */
public record UserResponse(Long id, String fullName, String email, String mobileNumber,
                           String role, boolean active, OffsetDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getMobileNumber(),
                u.getRole().name(), u.isActive(), u.getCreatedAt());
    }
}
