package edu.cit.lobitana.ajapartelle.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Profile and password management against a real PostgreSQL 16 database:
 * FR-072 (own profile), FR-073 (change password), FR-074 (forgot password), FR-075 (reset password)
 * and FR-045 (deactivated accounts). Only the mail sender is mocked.
 * Skipped automatically when Docker is not available; it always runs in CI (NFR-26).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=" + ProfileAndPasswordIntegrationTest.JWT_SECRET,
        "app.jwt.expiration-minutes=480",
        "app.owner.email=owner@test.local",
        "app.owner.password=OwnerPass123",
        "app.owner.full-name=Test Owner",
        "app.owner.mobile=09170000000",
        "app.mail.from=noreply@test.local",
        "app.frontend-url=http://frontend.test"
})
@AutoConfigureMockMvc
class ProfileAndPasswordIntegrationTest {

    static final String JWT_SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String PASSWORD = "Secret123";
    private static final String NEW_PASSWORD = "Changed456";
    private static final String PROFILE = "/api/v1/me/profile";
    private static final String CHANGE = "/api/v1/me/password";
    private static final String FORGOT = "/api/v1/auth/forgot-password";
    private static final String RESET = "/api/v1/auth/reset-password";
    private static final String SENT_MESSAGE = "If an account exists, we've sent a link";
    private static final Pattern LINK =
            Pattern.compile("http://frontend\\.test/reset-password\\?token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ApplicationContext context;
    @MockBean JavaMailSender mail;

    // ---------- helpers ----------

    private record Account(long id, String email) {}

    private ResultActions send(MockHttpServletRequestBuilder request, Object body, Cookie cookie) throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        if (cookie != null) request.cookie(cookie);
        return mvc.perform(request);
    }

    private Account newGuest() throws Exception {
        return newGuest("Juan Dela Cruz");
    }

    private Account newGuest(String fullName) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.local";
        MvcResult res = send(post("/api/v1/auth/register"), Map.of("fullName", fullName, "email", email,
                "mobileNumber", "09171234567", "password", PASSWORD, "confirmPassword", PASSWORD), null)
                .andExpect(status().isCreated()).andReturn();
        return new Account(mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong(), email);
    }

    private Account newStaff(Cookie owner) throws Exception {
        String email = "staff-" + UUID.randomUUID() + "@test.local";
        MvcResult res = send(post("/api/v1/owner/staff"), Map.of("fullName", "Front Desk", "email", email,
                "mobileNumber", "09181234567", "password", PASSWORD), owner)
                .andExpect(status().isCreated()).andReturn();
        return new Account(mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong(), email);
    }

    private void setActive(Cookie owner, Account staff, boolean active) throws Exception {
        send(patch("/api/v1/owner/staff/" + staff.id()), Map.of("active", active), owner).andExpect(status().isOk());
    }

    private ResultActions tryLogin(String email, String password) throws Exception {
        return send(post("/api/v1/auth/login"), Map.of("email", email, "password", password), null);
    }

    private Cookie login(String email, String password) throws Exception {
        return cookieFrom(tryLogin(email, password).andExpect(status().isOk()).andReturn());
    }

    private Cookie ownerLogin() throws Exception {
        return login("owner@test.local", "OwnerPass123");
    }

    private static Cookie cookieFrom(MvcResult res) {
        String header = res.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).startsWith("AJ_TOKEN=").contains("HttpOnly", "SameSite=Strict");
        return new Cookie("AJ_TOKEN", header.substring("AJ_TOKEN=".length(), header.indexOf(';')));
    }

    private void assertSession(Cookie cookie, int expectedStatus) throws Exception {
        mvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().is(expectedStatus));
    }

    private ResultActions changePassword(Cookie cookie, String current, String next, String confirm) throws Exception {
        return send(post(CHANGE),
                Map.of("currentPassword", current, "newPassword", next, "confirmPassword", confirm), cookie);
    }

    private ResultActions reset(String token, String next, String confirm) throws Exception {
        return send(post(RESET), Map.of("token", token, "newPassword", next, "confirmPassword", confirm), null);
    }

    /** The mail executor has one thread, so a no-op that has finished means all earlier work has finished. */
    private void awaitMail() throws Exception {
        context.getBean("mailExecutor", ThreadPoolTaskExecutor.class).submit(() -> { }).get(20, TimeUnit.SECONDS);
    }

    private void forgot(String email) throws Exception {
        send(post(FORGOT), Map.of("email", email), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SENT_MESSAGE));
        awaitMail();
    }

    private List<SimpleMailMessage> sentMail() {
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, atLeast(0)).send(sent.capture());
        return sent.getAllValues();
    }

    private String lastToken() {
        List<SimpleMailMessage> sent = sentMail();
        assertThat(sent).isNotEmpty();
        Matcher link = LINK.matcher(sent.get(sent.size() - 1).getText());
        assertThat(link.find()).isTrue();
        return link.group(1);
    }

    private String requestToken(Account account) throws Exception {
        forgot(account.email());
        return lastToken();
    }

    private int tokenCount(Account account) {
        return jdbc.queryForObject("select count(*) from password_reset_tokens where user_id = ?", Integer.class, account.id());
    }

    private String passwordHash(Account account) {
        return jdbc.queryForObject("select password_hash from users where id = ?", String.class, account.id());
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------- profile (FR-072) ----------

    @Test
    void userGetsOwnProfileWithoutPasswordData() throws Exception {
        Account guest = newGuest();
        mvc.perform(get(PROFILE).cookie(login(guest.email(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guest.id()))
                .andExpect(jsonPath("$.email").value(guest.email()))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.fullName").value("Juan Dela Cruz"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.tokenVersion").doesNotExist());
    }

    @Test
    void validProfileUpdateIsSavedAndKeepsTheSession() throws Exception {
        Account guest = newGuest();
        Cookie session = login(guest.email(), PASSWORD);
        send(put(PROFILE), Map.of("fullName", "  Maria Santos  ", "mobileNumber", "09998887777"), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Maria Santos"));
        assertThat(jdbc.queryForMap("select full_name, mobile_number from users where id = ?", guest.id()))
                .containsEntry("full_name", "Maria Santos").containsEntry("mobile_number", "09998887777");
        assertSession(session, 200);
    }

    @Test
    void staffAndOwnerCanUseTheirOwnProfile() throws Exception {
        Cookie owner = ownerLogin();
        Cookie staff = login(newStaff(owner).email(), PASSWORD);
        mvc.perform(get(PROFILE).cookie(staff)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STAFF"));
        mvc.perform(get(PROFILE).cookie(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OWNER"));
        send(put(PROFILE), Map.of("fullName", "Desk Officer", "mobileNumber", "09181112222"), staff).andExpect(status().isOk());
        send(put(PROFILE), Map.of("fullName", "Test Owner", "mobileNumber", "09170000000"), owner).andExpect(status().isOk());
    }

    @Test
    void invalidMobileReturns400() throws Exception {
        Cookie session = login(newGuest().email(), PASSWORD);
        for (String mobile : List.of("12345", "08171234567", "091712345678", "0917123456a", "")) {
            send(put(PROFILE), Map.of("fullName", "Juan Dela Cruz", "mobileNumber", mobile), session)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.mobileNumber").exists());
        }
    }

    @Test
    void invalidFullNameReturns400() throws Exception {
        Cookie session = login(newGuest().email(), PASSWORD);
        for (String name : List.of("", "   ", " J ", "x".repeat(101))) {
            send(put(PROFILE), Map.of("fullName", name, "mobileNumber", "09171234567"), session)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());
        }
    }

    @Test
    void profileAndPasswordRequireSignIn() throws Exception {
        mvc.perform(get(PROFILE)).andExpect(status().isUnauthorized());
        send(put(PROFILE), Map.of("fullName", "No One", "mobileNumber", "09171234567"), null)
                .andExpect(status().isUnauthorized());
        changePassword(null, PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void emailRoleAndOtherFieldsAreReadOnly() throws Exception {
        Account guest = newGuest();
        Map<String, Object> body = new HashMap<>(Map.of("fullName", "Juan Dela Cruz", "mobileNumber", "09171234567"));
        body.putAll(Map.of("email", "stolen@test.local", "role", "OWNER", "active", false, "tokenVersion", 99));
        send(put(PROFILE), body, login(guest.email(), PASSWORD)).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("select email, role, active, token_version from users where id = ?", guest.id()))
                .containsEntry("email", guest.email()).containsEntry("role", "GUEST")
                .containsEntry("active", true).containsEntry("token_version", 0);
    }

    @Test
    void noEndpointCanEditAnotherUser() throws Exception {
        Account attacker = newGuest();
        Account victim = newGuest("Untouched Victim");
        Cookie session = login(attacker.email(), PASSWORD);
        Map<String, Object> body = Map.of("id", victim.id(), "userId", victim.id(), "email", victim.email(),
                "fullName", "Changed Name", "mobileNumber", "09990000000");

        // Identifiers in the body or query are ignored: only the caller's own row changes.
        send(put(PROFILE + "?id=" + victim.id() + "&userId=" + victim.id()), body, session)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(attacker.id()));
        // There is no profile route that takes an ID.
        for (String path : List.of(PROFILE + "/" + victim.id(), "/api/v1/users/" + victim.id() + "/profile")) {
            send(put(path), body, session).andExpect(status().isNotFound());
        }
        send(patch("/api/v1/owner/staff/" + victim.id()), Map.of("active", false), session)
                .andExpect(status().isForbidden());

        // The owner's only endpoint with a user ID changes a staff member's active flag and nothing else.
        Cookie owner = ownerLogin();
        send(patch("/api/v1/owner/staff/" + victim.id()), Map.of("active", false), owner).andExpect(status().isBadRequest());
        Account staff = newStaff(owner);
        send(patch("/api/v1/owner/staff/" + staff.id()), Map.of("active", true, "fullName", "Changed Name"), owner)
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Front Desk"));

        assertThat(jdbc.queryForMap("select full_name, mobile_number, active from users where id = ?", victim.id()))
                .containsEntry("full_name", "Untouched Victim").containsEntry("mobile_number", "09171234567")
                .containsEntry("active", true);
    }

    // ---------- change password (FR-073) ----------

    @Test
    void wrongCurrentPasswordReturns400AndChangesNothing() throws Exception {
        Account guest = newGuest();
        Cookie session = login(guest.email(), PASSWORD);
        changePassword(session, "WrongPass999", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
        assertSession(session, 200);
        login(guest.email(), PASSWORD);
    }

    @Test
    void mismatchedConfirmationReturns400() throws Exception {
        changePassword(login(newGuest().email(), PASSWORD), PASSWORD, NEW_PASSWORD, "Different789")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword").exists());
    }

    @Test
    void newPasswordSameAsTheOldOneReturns400() throws Exception {
        changePassword(login(newGuest().email(), PASSWORD), PASSWORD, PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void newPasswordFollowsRegistrationRules() throws Exception {
        Cookie session = login(newGuest().email(), PASSWORD);
        for (String weak : List.of("Ab1", "lettersonly", "1234567890", "a1" + "x".repeat(71))) {
            changePassword(session, PASSWORD, weak, weak)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        }
    }

    @Test
    void passwordChangeSignsOutOtherSessionsAndRenewsThisOne() throws Exception {
        Account guest = newGuest();
        Cookie thisSession = login(guest.email(), PASSWORD);
        Cookie otherSession = login(guest.email(), PASSWORD);

        Cookie renewed = cookieFrom(changePassword(thisSession, PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isOk()).andReturn());

        assertSession(otherSession, 401);
        assertSession(thisSession, 401);
        assertSession(renewed, 200);
        tryLogin(guest.email(), PASSWORD).andExpect(status().isUnauthorized());
        login(guest.email(), NEW_PASSWORD);
        assertThat(passwordHash(guest)).startsWith("$2").doesNotContain(NEW_PASSWORD);
        assertThat(encoder.matches(NEW_PASSWORD, passwordHash(guest))).isTrue();
    }

    @Test
    void passwordChangeInvalidatesAnUnusedResetLink() throws Exception {
        Account guest = newGuest();
        String token = requestToken(guest);
        changePassword(login(guest.email(), PASSWORD), PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        reset(token, "Another789", "Another789").andExpect(status().isBadRequest());
    }

    // ---------- forgot password (FR-074) ----------

    @Test
    void knownAndUnknownEmailsGetTheSameResponse() throws Exception {
        Account guest = newGuest();
        String known = send(post(FORGOT), Map.of("email", guest.email()), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        awaitMail();
        assertThat(sentMail()).hasSize(1);

        String unknown = send(post(FORGOT), Map.of("email", "nobody-" + UUID.randomUUID() + "@test.local"), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        awaitMail();
        assertThat(unknown).isEqualTo(known).contains(SENT_MESSAGE);
        assertThat(sentMail()).hasSize(1);
    }

    @Test
    void malformedEmailReturns400() throws Exception {
        send(post(FORGOT), Map.of("email", "not-an-email"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void tokenIsStoredOnlyAsAHashAndExpiresIn30Minutes() throws Exception {
        Account guest = newGuest();
        String token = requestToken(guest);
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);

        Map<String, Object> row = jdbc.queryForMap(
                "select token_hash, used_at, extract(epoch from expires_at - created_at)::int as ttl "
                        + "from password_reset_tokens where user_id = ?", guest.id());
        assertThat(row).containsEntry("token_hash", sha256(token)).containsEntry("ttl", 1800).containsEntry("used_at", null);
        assertThat(row.toString()).doesNotContain(token);
    }

    @Test
    void emailIsPlainTextWithFirstNameOnlyAndManilaTime() throws Exception {
        Account guest = newGuest();
        forgot(guest.email().toUpperCase(Locale.ROOT));

        SimpleMailMessage email = sentMail().get(0);
        assertThat(email.getTo()).containsExactly(guest.email());
        assertThat(email.getFrom()).isEqualTo("noreply@test.local");
        assertThat(email.getSubject()).contains("password");

        OffsetDateTime expires = jdbc.queryForObject(
                "select expires_at from password_reset_tokens where user_id = ?", OffsetDateTime.class, guest.id());
        String manila = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)
                .format(expires.atZoneSameInstant(ZoneId.of("Asia/Manila")));
        assertThat(email.getText())
                .contains("Hello Juan,", manila, "Philippine time")
                .doesNotContain("Dela Cruz", guest.email(), "09171234567");
    }

    @Test
    void nameThatLooksLikeALinkIsNotPutInTheEmail() throws Exception {
        forgot(newGuest("http://phish.example Click").email());
        assertThat(sentMail().get(0).getText()).contains("Hello,").doesNotContain("phish.example");
    }

    @Test
    void newRequestInvalidatesTheEarlierLink() throws Exception {
        Account guest = newGuest();
        String first = requestToken(guest);
        String second = requestToken(guest);
        assertThat(second).isNotEqualTo(first);
        reset(first, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        reset(second, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void atMostThreeLinksPerEmailPerHour() throws Exception {
        Account guest = newGuest();
        for (int i = 0; i < 3; i++) forgot(guest.email());
        String third = lastToken();
        forgot(guest.email()); // same response, but nothing is created or sent
        assertThat(sentMail()).hasSize(3);
        assertThat(tokenCount(guest)).isEqualTo(3);

        jdbc.update("update password_reset_tokens set created_at = created_at - interval '61 minutes' where user_id = ?", guest.id());
        forgot(guest.email());
        assertThat(sentMail()).hasSize(4);
        assertThat(lastToken()).isNotEqualTo(third);
    }

    @Test
    void refusedFourthRequestLeavesTheThirdLinkWorking() throws Exception {
        Account guest = newGuest();
        for (int i = 0; i < 3; i++) forgot(guest.email());
        String third = lastToken();
        forgot(guest.email());
        reset(third, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void deactivatedAccountGetsNoEmail() throws Exception {
        Cookie owner = ownerLogin();
        Account staff = newStaff(owner);
        setActive(owner, staff, false);
        forgot(staff.email());
        verify(mail, never()).send(any(SimpleMailMessage.class));
        assertThat(tokenCount(staff)).isZero();
    }

    @Test
    void mailFailureStillReturnsTheSameResponse() throws Exception {
        doThrow(new MailSendException("SMTP is down")).when(mail).send(any(SimpleMailMessage.class));
        Account guest = newGuest();
        forgot(guest.email());
        login(guest.email(), PASSWORD);
    }

    @Test
    void responseDoesNotWaitForTheMailServer() throws Exception {
        CountDownLatch responded = new CountDownLatch(1);
        AtomicBoolean respondedFirst = new AtomicBoolean();
        doAnswer(call -> {
            respondedFirst.set(responded.await(5, TimeUnit.SECONDS));
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));

        send(post(FORGOT), Map.of("email", newGuest().email()), null).andExpect(status().isOk());
        responded.countDown();
        awaitMail();
        assertThat(respondedFirst).isTrue();
    }

    // ---------- reset password (FR-075) ----------

    @Test
    void resetWorksOnceAndTheOldPasswordFailsAfterwards() throws Exception {
        Account guest = newGuest();
        String token = requestToken(guest);

        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select used_at is not null from password_reset_tokens where user_id = ?",
                Boolean.class, guest.id())).isTrue();
        assertThat(passwordHash(guest)).startsWith("$2");
        tryLogin(guest.email(), PASSWORD).andExpect(status().isUnauthorized());
        login(guest.email(), NEW_PASSWORD);
    }

    @Test
    void reusedTokenReturns400() throws Exception {
        Account guest = newGuest();
        String token = requestToken(guest);
        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        reset(token, "Another789", "Another789")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        login(guest.email(), NEW_PASSWORD);
    }

    @Test
    void expiredTokenReturns400() throws Exception {
        Account guest = newGuest();
        String token = requestToken(guest);
        jdbc.update("update password_reset_tokens set expires_at = now() - interval '1 second' where user_id = ?", guest.id());
        reset(token, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        login(guest.email(), PASSWORD);
    }

    @Test
    void unknownBlankAndMissingTokensGetTheSameGenericError() throws Exception {
        String message = "This reset link is invalid or has expired. Please request a new one.";
        for (String token : List.of("not-a-real-token", "")) {
            reset(token, NEW_PASSWORD, NEW_PASSWORD)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        }
        send(post(RESET), Map.of("newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void invalidNewPasswordDoesNotUseUpTheToken() throws Exception {
        String token = requestToken(newGuest());
        reset(token, NEW_PASSWORD, "Different789")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.confirmPassword").exists());
        reset(token, "short", "short")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void resetSignsOutAllOldSessions() throws Exception {
        Account guest = newGuest();
        Cookie one = login(guest.email(), PASSWORD);
        Cookie two = login(guest.email(), PASSWORD);
        reset(requestToken(guest), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        assertSession(one, 401);
        assertSession(two, 401);
    }

    @Test
    void simultaneousResetsWithOneTokenSucceedOnlyOnce() throws Exception {
        String token = requestToken(newGuest());
        int attempts = 6;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(attempts)) {
            for (int i = 0; i < attempts; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return reset(token, NEW_PASSWORD, NEW_PASSWORD).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> codes = new ArrayList<>();
            for (Future<Integer> result : results) codes.add(result.get(30, TimeUnit.SECONDS));
            assertThat(codes).containsOnly(200, 400).containsOnlyOnce(200);
        }
    }

    @Test
    void accountDeactivatedAfterTheEmailCannotReset() throws Exception {
        Cookie owner = ownerLogin();
        Account staff = newStaff(owner);
        String token = requestToken(staff);
        setActive(owner, staff, false);
        reset(token, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    // ---------- 72-byte bcrypt limit (FR-071, FR-073, FR-075; CVE-2025-22228) ----------

    private static final String TOO_LONG =
            "Password is too long. The limit is 72 bytes: 72 plain letters or numbers, fewer with accented letters or emoji";
    private static final String BYTES_72 = "a1" + "x".repeat(70);
    private static final String BYTES_73 = "a1" + "x".repeat(71);
    /** 38 characters but 74 bytes, so a character count alone would let it through. */
    private static final String MULTIBYTE_74 = "a1" + "\u00e9".repeat(36);

    private ResultActions register(String email, String fullName, String password) throws Exception {
        return send(post("/api/v1/auth/register"), Map.of("fullName", fullName, "email", email,
                "mobileNumber", "09171234567", "password", password, "confirmPassword", password), null);
    }

    private static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    @Test
    void registrationRejectsPasswordOver72Bytes() throws Exception {
        for (String password : List.of(BYTES_73, MULTIBYTE_74)) {
            register(randomEmail(), "Juan Dela Cruz", password)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").value(TOO_LONG));
        }
    }

    @Test
    void staffCreationRejectsPasswordOver72Bytes() throws Exception {
        Cookie owner = ownerLogin();
        for (String password : List.of(BYTES_73, MULTIBYTE_74)) {
            send(post("/api/v1/owner/staff"), Map.of("fullName", "Front Desk", "email", randomEmail(),
                    "mobileNumber", "09181234567", "password", password), owner)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").value(TOO_LONG));
        }
    }

    @Test
    void changeRejectsPasswordOver72Bytes() throws Exception {
        Cookie session = login(newGuest().email(), PASSWORD);
        for (String password : List.of(BYTES_73, MULTIBYTE_74)) {
            changePassword(session, PASSWORD, password, password)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPassword").value(TOO_LONG));
        }
    }

    @Test
    void resetRejectsPasswordOver72BytesWithoutUsingUpTheToken() throws Exception {
        String token = requestToken(newGuest());
        for (String password : List.of(BYTES_73, MULTIBYTE_74)) {
            reset(token, password, password)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPassword").value(TOO_LONG));
        }
        reset(token, BYTES_72, BYTES_72).andExpect(status().isOk());
    }

    @Test
    void passwordsOfExactly72BytesWork() throws Exception {
        for (String password : List.of(BYTES_72, "a1" + "\u00e9".repeat(35))) {
            String email = randomEmail();
            register(email, "Juan Dela Cruz", password).andExpect(status().isCreated());
            login(email, password);
        }
    }

    @Test
    void wrongSuffixAfter72BytesIsNotAcceptedAtLogin() throws Exception {
        String email = randomEmail();
        register(email, "Juan Dela Cruz", BYTES_72).andExpect(status().isCreated());
        for (String suffix : List.of("x", "WRONG", "\u00e9")) {
            tryLogin(email, BYTES_72 + suffix)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        login(email, BYTES_72);
    }

    @Test
    void wrongSuffixIsNotAcceptedAsTheCurrentPassword() throws Exception {
        String email = randomEmail();
        register(email, "Juan Dela Cruz", BYTES_72).andExpect(status().isCreated());
        changePassword(login(email, BYTES_72), BYTES_72 + "WRONG", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
    }

    @Test
    void longPasswordForUnknownEmailGetsTheGenericError() throws Exception {
        tryLogin(randomEmail(), BYTES_73)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void accountWithATruncatedLegacyHashRecoversByReset() throws Exception {
        Account guest = newGuest();
        // What older versions stored for a longer password: the hash of its first 72 bytes.
        jdbc.update("update users set password_hash = ? where id = ?", encoder.encode(BYTES_72), guest.id());
        tryLogin(guest.email(), BYTES_73).andExpect(status().isUnauthorized());
        reset(requestToken(guest), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        login(guest.email(), NEW_PASSWORD);
    }

    // ---------- registration name (FR-071) ----------

    @Test
    void registrationTrimsTheNameBeforeValidating() throws Exception {
        for (String name : List.of(" J ", "   ", "  J")) {
            register(randomEmail(), name, PASSWORD)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());
        }
        register(randomEmail(), "  " + "x".repeat(100) + "  ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("x".repeat(100)));
    }

    @Test
    void staffCreationTrimsTheNameBeforeValidating() throws Exception {
        Cookie owner = ownerLogin();
        for (String name : List.of(" J ", "   ", "  J")) {
            send(post("/api/v1/owner/staff"), Map.of("fullName", name, "email", randomEmail(),
                    "mobileNumber", "09181234567", "password", PASSWORD), owner)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());
        }
        send(post("/api/v1/owner/staff"), Map.of("fullName", "  " + "x".repeat(100) + "  ", "email", randomEmail(),
                "mobileNumber", "09181234567", "password", PASSWORD), owner)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("x".repeat(100)));
    }

    // ---------- token version (FR-045, FR-073, FR-075) ----------

    @Test
    void reactivationDoesNotReviveOldSessions() throws Exception {
        Cookie owner = ownerLogin();
        Account staff = newStaff(owner);
        Cookie old = login(staff.email(), PASSWORD);
        setActive(owner, staff, false);
        setActive(owner, staff, true);
        assertSession(old, 401);
        assertSession(login(staff.email(), PASSWORD), 200);
    }

    @Test
    void tokenWithoutAVersionClaimIsRejected() throws Exception {
        Account guest = newGuest();
        String legacy = Jwts.builder().id(UUID.randomUUID().toString()).subject(String.valueOf(guest.id()))
                .claim("role", "GUEST").expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertSession(new Cookie("AJ_TOKEN", legacy), 401);
    }

    // ---------- error handling (Appendix E) ----------

    @Test
    void requestErrorsAreNotReportedAsSignedOut() throws Exception {
        Cookie owner = ownerLogin();
        mvc.perform(get("/api/v1/no-such-path").cookie(owner))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(delete(PROFILE).cookie(owner))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").exists());
        mvc.perform(put(PROFILE).cookie(owner).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").exists());
        send(patch("/api/v1/owner/staff/abc"), Map.of("active", true), owner)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
