package edu.cit.lobitana.ajapartelle.user.dto;

import jakarta.validation.constraints.*;

/** The editable profile fields (FR-072). Email and role are deliberately absent. */
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = AccountRules.NAME_MIN, max = AccountRules.NAME_MAX, message = AccountRules.NAME_SIZE)
        String fullName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = AccountRules.MOBILE_REGEX, message = AccountRules.MOBILE_FORMAT)
        String mobileNumber
) {
    public UpdateProfileRequest {
        // Trim before validation so that " J " is not accepted as a three-character name.
        if (fullName != null) fullName = fullName.trim();
    }
}
