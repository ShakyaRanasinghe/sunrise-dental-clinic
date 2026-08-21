package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A way of getting a message to a patient — the product interface of the Factory Method
 * pattern. Concrete channels are handed out by {@link NotificationChannelFactory}.
 *
 * <p>Adding a real transport later means writing one more implementation and registering it in
 * {@code AppContext}. No caller changes, which is the reason the pattern is here.</p>
 */
public interface NotificationChannel {

    Logger LOG = Logger.getLogger(NotificationChannel.class.getName());

    /** Which channel this is. The factory indexes by it. */
    ChannelType type();

    /**
     * Try to send. Implementations may throw — {@link #dispatch} is what callers use.
     *
     * @return what became of it
     */
    DispatchResult send(String recipient, String subject, String body);

    /**
     * Send, and never throw.
     *
     * <p>The interface used to say "never throws" and leave each implementation to honour it.
     * One of the two did, with a try-catch; the other did not, so the guarantee held by
     * coincidence. A promise every implementer has to remember is the
     * discipline-not-construction problem, so it is made here instead: implementations write
     * {@link #send} and may throw, callers use this, and the contract cannot be broken by a
     * channel added later.</p>
     *
     * <p>It matters because of FR-NOT-04: a booking is already committed by the time anything
     * is sent about it, so an exception escaping here would report a failure for an appointment
     * that exists — and the patient would book again.</p>
     */
    default DispatchResult dispatch(String recipient, String subject, String body) {
        try {
            DispatchResult result = send(recipient, subject, body);
            return result == null
                    ? DispatchResult.failed(type(), "the channel returned nothing")
                    : result;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "channel_threw type=" + type() + " recipient=" + recipient, e);
            return DispatchResult.failed(type(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
