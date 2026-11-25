package edu.cit.lobitana.ajapartelle.user.dto;

import edu.cit.lobitana.ajapartelle.auth.dto.ChangePasswordRequest;
import edu.cit.lobitana.ajapartelle.auth.dto.ResetPasswordRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Every form that sets a password limits it to 72 UTF-8 bytes (FR-071, FR-073, FR-075). Runs without Docker. */
class PasswordLengthValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<Object> forms(String password) {
        return List.of(
                new RegisterRequest("Juan Dela Cruz", "juan@test.local", "09171234567", password, password),
                new CreateStaffRequest("Front Desk", "desk@test.local", "09181234567", password),
                new ChangePasswordRequest("Current123", password, password),
                new ResetPasswordRequest("token", password, password));
    }

    @Test
    void over72BytesIsRejectedWithOneClearMessage() {
        for (String password : List.of("a1" + "x".repeat(71), "a1" + "\u00e9".repeat(36), "a1" + "\ud83d\ude00".repeat(18))) {
            for (Object form : forms(password)) {
                assertThat(VALIDATOR.validate(form)).as(form.getClass().getSimpleName())
                        .singleElement()
                        .satisfies(v -> assertThat(v.getMessage()).contains("72 bytes"));
            }
        }
    }

    @Test
    void exactly72BytesIsAccepted() {
        for (String password : List.of("a1" + "x".repeat(70), "a1" + "\u00e9".repeat(35))) {
            for (Object form : forms(password)) assertThat(VALIDATOR.validate(form)).isEmpty();
        }
    }

    @Test
    void under8CharactersIsStillRejected() {
        for (Object form : forms("abc1234")) assertThat(VALIDATOR.validate(form)).isNotEmpty();
    }
}
