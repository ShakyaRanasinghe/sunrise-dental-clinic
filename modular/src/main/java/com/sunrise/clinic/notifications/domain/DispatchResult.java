package com.sunrise.clinic.notifications.domain;

/**
 * What happened when a channel was asked to send something. Immutable, and never an exception.
 *
 * <h2>Why this carries a status and not a boolean</h2>
 *
 * <p>It used to be {@code (boolean sent, String reason, …)}, and both channels returned
 * {@code sent = true} with {@code reason = "logged"} — having sent nothing at all. A caller
 * reading the boolean would record that the patient had been told, when the message had only
 * been written to a log file.</p>
 *
 * <p>{@link NotificationStatus} already draws that distinction, and drawing it twice in two
 * shapes is how the two came apart. So the result carries the status, {@link #sent()} is derived
 * from it, and a channel cannot claim delivery it did not achieve — the only way to return
 * {@code sent() == true} is to return {@code SENT}.</p>
 *
 * @param status  what became of it
 * @param reason  a stable code for a caller to branch on: {@code sent}, {@code logged},
 *                {@code send_failed}
 * @param detail  for a log, and never shown to a patient
 * @param channel which channel handled it
 */
public record DispatchResult(NotificationStatus status, String reason, String detail,
                             ChannelType channel) {

    /** It reached the recipient. Nothing else counts. */
    public static DispatchResult sent(ChannelType channel, String detail) {
        return new DispatchResult(NotificationStatus.SENT, "sent", detail, channel);
    }

    /**
     * It was recorded and not sent, because no transport is configured.
     *
     * <p>The honest outcome on a development machine and at a clinic with no mail relay. It is
     * not a failure — nothing went wrong — and it is not a delivery.</p>
     */
    public static DispatchResult logged(ChannelType channel, String detail) {
        return new DispatchResult(NotificationStatus.LOGGED, "logged", detail, channel);
    }

    /** The transport was there and refused, or broke. */
    public static DispatchResult failed(ChannelType channel, String detail) {
        return new DispatchResult(NotificationStatus.FAILED, "send_failed", detail, channel);
    }

    /** @return true only if the patient can be assumed to have received it. */
    public boolean sent() {
        return status.reachedTheRecipient();
    }
}
