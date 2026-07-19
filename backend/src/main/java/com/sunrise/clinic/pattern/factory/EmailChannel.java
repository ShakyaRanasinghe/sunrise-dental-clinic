package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email channel. Sends a real email via SMTP when configured
 * ({@code clinic.mail.enabled=true} + {@code spring.mail.*}); otherwise it
 * safely logs the message so the system runs (and tests pass) with no
 * external account. The send never throws — failures are captured in the
 * returned {@link DispatchResult}.
 */
@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public EmailChannel(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${clinic.mail.enabled:false}") boolean enabled,
            @Value("${clinic.mail.from:no-reply@sunrisedental.lk}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public DispatchResult send(String recipient, String subject, String body) {
        JavaMailSender sender = enabled ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            // Offline / not configured: log instead of failing.
            log.info("email_logged recipient={} subject={}", recipient, subject);
            return new DispatchResult(true, "logged", "SMTP disabled — email logged", ChannelType.EMAIL);
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(recipient);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.info("email_sent recipient={} subject={}", recipient, subject);
            return new DispatchResult(true, "sent", "email sent", ChannelType.EMAIL);
        } catch (Exception e) {
            // Best-effort: capture, never propagate.
            log.warn("email_failed recipient={} error={}", recipient, e.toString());
            return new DispatchResult(false, "send_failed", e.getMessage(), ChannelType.EMAIL);
        }
    }
}
