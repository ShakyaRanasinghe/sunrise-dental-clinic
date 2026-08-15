package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Email channel.
 *
 * <p>The mail-sending helper this class previously used came from the application
 * framework, which is no longer a dependency. Rather than pull in a replacement
 * mail library, the channel records the message it would send: the confirmation is
 * written to the log and persisted as a {@code Notification} row by the observer
 * that calls it, so the patient-facing audit of "what did we tell them, and when"
 * is unchanged.</p>
 *
 * <p>Adding real SMTP later means writing one more implementation of
 * {@link NotificationChannel} and registering it with the factory — no caller
 * changes. That substitutability is the reason the Factory Method pattern is used
 * here in the first place.</p>
 */
public class EmailChannel implements NotificationChannel {

    private static final Logger log = Logger.getLogger(EmailChannel.class.getName());

    private final String from;

    public EmailChannel(String from) {
        this.from = from;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public DispatchResult send(String recipient, String subject, String body) {
        // Best-effort by contract: this method must never throw, because a failed
        // notification must not roll back the booking that triggered it.
        try {
            log.log(Level.INFO, "email_logged from={0} recipient={1} subject={2}",
                    new Object[]{from, recipient, subject});
            return new DispatchResult(true, "logged",
                    "no mail transport configured — email recorded", ChannelType.EMAIL);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "email_failed recipient=" + recipient, e);
            return new DispatchResult(false, "send_failed", e.getMessage(), ChannelType.EMAIL);
        }
    }
}
