package edu.cit.lobitana.ajapartelle.user.dto;

import edu.cit.lobitana.ajapartelle.common.validation.MaxUtf8Bytes;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = AccountRules.NAME_MIN, max = AccountRules.NAME_MAX, message = AccountRules.NAME_SIZE)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email is too long")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = AccountRules.MOBILE_REGEX, message = AccountRules.MOBILE_FORMAT)
        String mobileNumber,

        @NotBlank(message = "Password is required")
        @Size(min = AccountRules.PASSWORD_MIN, message = AccountRules.PASSWORD_TOO_SHORT)
        @MaxUtf8Bytes(value = AccountRules.PASSWORD_MAX_BYTES, message = AccountRules.PASSWORD_TOO_LONG)
        @Pattern(regexp = AccountRules.PASSWORD_REGEX, message = AccountRules.PASSWORD_FORMAT)
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword
) {
    public RegisterRequest {
        // Trim before validation, as in UpdateProfileRequest, so that " J " is not a valid name.
        if (fullName != null) fullName = fullName.trim();
    }
}
