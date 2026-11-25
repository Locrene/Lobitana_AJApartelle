package edu.cit.lobitana.ajapartelle.auth.dto;

import edu.cit.lobitana.ajapartelle.user.dto.AccountRules;

import edu.cit.lobitana.ajapartelle.common.validation.MaxUtf8Bytes;

import jakarta.validation.constraints.*;

/** FR-075. The token has no constraint: a missing one gets the same generic error as an invalid one. */
public record ResetPasswordRequest(
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = AccountRules.PASSWORD_MIN, message = AccountRules.PASSWORD_TOO_SHORT)
        @MaxUtf8Bytes(value = AccountRules.PASSWORD_MAX_BYTES, message = AccountRules.PASSWORD_TOO_LONG)
        @Pattern(regexp = AccountRules.PASSWORD_REGEX, message = AccountRules.PASSWORD_FORMAT)
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmPassword
) {}
