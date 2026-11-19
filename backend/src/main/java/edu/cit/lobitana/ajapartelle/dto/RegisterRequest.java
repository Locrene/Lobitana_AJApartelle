package edu.cit.lobitana.ajapartelle.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be 2 to 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email is too long")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^09\\d{9}$", message = "Mobile number must be in the format 09XXXXXXXXX (11 digits)")
        String mobileNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword
) {}
