package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Email, recorded rather than sent.
 *
 * <p>The mail helper this used came from the application framework, which is not a dependency
 * any more, and pulling in a mail library to replace it would be a sixth dependency bought for
 * a feature the brief does not ask for. So the channel records exactly what it would have sent
 * and returns {@code LOGGED} — the observer persists that as a {@code Notification} row, so
 * "what did we tell them, and when" is answerable even though nothing left the building.</p>
 *
 * <p><b>It returns {@code LOGGED}, not {@code SENT}.</b> The previous version returned a boolean
 * {@code true} with the reason "logged", so a caller reading the boolean recorded a delivery
 * that had not happened. Saying it plainly costs nothing and stops a report claiming patients
 * were informed when they were not.</p>
 *
 * <p>Real SMTP is one more implementation of {@link NotificationChannel} and one line in
 * {@code AppContext}. Nothing that sends would change.</p>
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
        if (recipient == null || recipient.isBlank()) {
            // A patient who registered without an email. Not a failure of the channel, and not
            // something to retry — there is nowhere to send it.
            return DispatchResult.failed(ChannelType.EMAIL, "the patient has no email address");
        }
        // The subject, never the body: the body names a patient and their appointment.
        log.log(Level.INFO, "email_logged from={0} recipient={1} subject={2}",
                new Object[] { from, recipient, subject });
        return DispatchResult.logged(ChannelType.EMAIL, "no mail transport configured");
    }
}
