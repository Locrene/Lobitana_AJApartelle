package edu.cit.lobitana.ajapartelle.auth.dto;

import edu.cit.lobitana.ajapartelle.user.dto.AccountRules;

import edu.cit.lobitana.ajapartelle.common.validation.MaxUtf8Bytes;

import jakarta.validation.constraints.*;

/** FR-073. */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = AccountRules.PASSWORD_MIN, message = AccountRules.PASSWORD_TOO_SHORT)
        @MaxUtf8Bytes(value = AccountRules.PASSWORD_MAX_BYTES, message = AccountRules.PASSWORD_TOO_LONG)
        @Pattern(regexp = AccountRules.PASSWORD_REGEX, message = AccountRules.PASSWORD_FORMAT)
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmPassword
) {}
