package edu.cit.lobitana.ajapartelle.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end auth and role checks against a real PostgreSQL 16 database (Flyway migrations included).
 * Covers FR-071 (register / sign in with bcrypt password), FR-042 (8-hour JWT), FR-043 (GUEST by default,
 * seeded OWNER), FR-044 (403 for wrong role), FR-045 (deactivated account refused), FR-046 (sign out),
 * FR-061 and FR-062 (owner manages staff, cannot deactivate self).
 * Skipped automatically when Docker is not available; it always runs in CI (NFR-26).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-that-is-definitely-longer-than-32-bytes",
        "app.jwt.expiration-minutes=480",
        "app.owner.email=owner@test.local",
        "app.owner.password=OwnerPass123",
        "app.owner.full-name=Test Owner",
        "app.owner.mobile=09170000000"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String OWNER_EMAIL = "owner@test.local";
    private static final String OWNER_PASSWORD = "OwnerPass123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // ---------- helpers ----------

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    private static String registerJson(String email, String mobile, String password, String confirm) {
        return """
                {"fullName":"Juan Dela Cruz","email":"%s","mobileNumber":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(email, mobile, password, confirm);
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "09171234567", "Secret123", "Secret123")))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = res.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("AJ_TOKEN=").contains("HttpOnly").contains("SameSite=Strict");
        String token = setCookie.substring("AJ_TOKEN=".length(), setCookie.indexOf(';'));
        return new Cookie("AJ_TOKEN", token);
    }

    // ---------- registration (FR-071, FR-043) ----------

    @Test
    void registerCreatesGuestAndNeverReturnsPassword() throws Exception {
        String email = uniqueEmail();
        JsonNode body = mapper.readTree(register(email).getResponse().getContentAsString());
        assertThat(body.get("role").asText()).isEqualTo("GUEST");
        assertThat(body.get("email").asText()).isEqualTo(email);
        assertThat(body.has("password")).isFalse();
        assertThat(body.has("passwordHash")).isFalse();
    }

    @Test
    void registerWithInvalidFieldsReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", "12345", "short", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.mobileNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registerWithDuplicateEmailReturns409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), "09171234567", "Secret123", "Secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    // ---------- login (FR-071, FR-042) ----------

    @Test
    void loginSucceedsAndSetsHttpOnlyCookie() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie cookie = login(email, "Secret123");
        mvc.perform(get("/api/v1/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("GUEST"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameGenericMessage() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"WrongPass999\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-%s\",\"password\":\"WrongPass999\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    // ---------- logout and 401 (FR-046) ----------

    @Test
    void logoutRevokesTheTokenServerSide() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie cookie = login(email, "Secret123");
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        // The old token is now rejected even though it has not expired.
        mvc.perform(get("/api/v1/auth/me").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutSignInReturns401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/guest/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- roles (FR-044, NFR-05) ----------

    @Test
    void guestCallingOwnerEndpointGets403() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie guest = login(email, "Secret123");
        mvc.perform(get("/api/v1/owner/dashboard").cookie(guest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/owner/staff").cookie(guest))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/guest/dashboard").cookie(guest))
                .andExpect(status().isOk());
    }

    // ---------- staff management and deactivation (FR-061, FR-062, FR-045) ----------

    @Test
    void deactivatedStaffIsRefusedEvenWithAValidToken() throws Exception {
        Cookie owner = login(OWNER_EMAIL, OWNER_PASSWORD);
        String staffEmail = uniqueEmail();
        MvcResult created = mvc.perform(post("/api/v1/owner/staff").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Front Desk","email":"%s","mobileNumber":"09181234567","password":"Staff1234"}
                                """.formatted(staffEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andReturn();
        long staffId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        Cookie staff = login(staffEmail, "Staff1234");
        mvc.perform(get("/api/v1/staff/dashboard").cookie(staff)).andExpect(status().isOk());

        mvc.perform(patch("/api/v1/owner/staff/" + staffId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Existing token no longer works (FR-045) ...
        mvc.perform(get("/api/v1/staff/dashboard").cookie(staff)).andExpect(status().isUnauthorized());
        // ... and a new sign-in is refused.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Staff1234\"}".formatted(staffEmail)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));

        // Reactivation restores access (FR-061).
        mvc.perform(patch("/api/v1/owner/staff/" + staffId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
        login(staffEmail, "Staff1234");
    }

    @Test
    void ownerCannotDeactivateOwnAccount() throws Exception {
        Cookie owner = login(OWNER_EMAIL, OWNER_PASSWORD);
        long ownerId = mapper.readTree(mvc.perform(get("/api/v1/auth/me").cookie(owner))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/v1/owner/staff/" + ownerId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_STAFF"));
        mvc.perform(get("/api/v1/owner/dashboard").cookie(owner)).andExpect(status().isOk());
    }

    // ---------- API docs (NFR-18) ----------

    @Test
    void openApiDocsArePublicAndListEveryEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AJ Apartelle API"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/forgot-password'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/reset-password'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/profile'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/profile'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/password'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/guest/dashboard'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/staff/dashboard'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/owner/dashboard'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/owner/staff'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/owner/staff'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/owner/staff/{id}'].patch").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
