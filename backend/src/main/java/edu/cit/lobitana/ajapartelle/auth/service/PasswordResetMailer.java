package edu.cit.lobitana.ajapartelle.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Emails reset links (FR-074, SRS D-08). The account lookup and the SMTP call both run in the background,
 * so neither the response nor its timing shows whether the account exists. The email is sent after the
 * token is committed. Logs never contain an address or a token.
 */
@Service
public class PasswordResetMailer {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);
    private static final DateTimeFormatter EXPIRY = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US);
    /**
     * Names are typed by whoever registered the address, so only a plain word is echoed into the email
     * (a trailing period is allowed, as in "Ma.").
     */
    private static final Pattern PLAIN_NAME = Pattern.compile("[\\p{L}\\p{M}'\u2019-]{1,40}\\.?");

    private final PasswordResetService resets;
    private final JavaMailSender mailSender;
    private final TaskExecutor executor;
    private final ZoneId zone;
    private final String from;
    private final String frontendUrl;

    public PasswordResetMailer(PasswordResetService resets, JavaMailSender mailSender,
                               @Qualifier("mailExecutor") TaskExecutor executor, ZoneId zone,
                               @Value("${app.mail.from}") String from,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.resets = resets;
        this.mailSender = mailSender;
        this.executor = executor;
        this.zone = zone;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** Returns immediately. */
    public void requestLink(String email) {
        try {
            executor.execute(() -> issueAndSend(email));
        } catch (TaskRejectedException e) {
            log.warn("Password reset request dropped: the mail queue is full");
        }
    }

    private void issueAndSend(String email) {
        try {
            resets.issue(email).ifPresent(link -> {
                mailSender.send(compose(link));
                log.info("Password reset email sent");
            });
        } catch (RuntimeException e) {
            // Class name only: SMTP error messages can contain email addresses.
            log.warn("Password reset email failed ({})", e.getClass().getSimpleName());
        }
    }

    private SimpleMailMessage compose(PasswordResetService.IssuedLink link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(link.email());
        message.setSubject("Reset your AJ Apartelle password");
        message.setText("""
                %s

                We received a request to reset your AJ Apartelle password. Open this link to choose a new one:

                %s/reset-password?token=%s

                The link works once, until %s (Philippine time). If you ask for another link, this one stops working.

                If you did not ask for this, you can ignore this email. Your password has not changed.

                AJ Apartelle
                """.formatted(greeting(link.fullName()), frontendUrl, link.token(),
                EXPIRY.format(link.expiresAt().atZoneSameInstant(zone))));
        return message;
    }

    private static String greeting(String fullName) {
        String first = fullName.trim().split("\\s+", 2)[0];
        return PLAIN_NAME.matcher(first).matches() ? "Hello " + first + "," : "Hello,";
    }
}
