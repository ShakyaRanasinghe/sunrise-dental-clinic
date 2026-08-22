package com.sunrise.clinic.notifications.domain;

/**
 * What a message was for.
 *
 * <p>Recorded explicitly rather than inferred from the channel. The reminder sweep asks "has a
 * reminder already gone out for this appointment", and answering that by looking for an SMS
 * would work only until somebody sent a reminder by email — at which point every patient would
 * get two.</p>
 */
public enum NotificationKind {

    /** Sent when the appointment is booked — FR-NOT-01. */
    CONFIRMATION,
    /** Sent the day before — FR-NOT-02. At most one per appointment. */
    REMINDER,
    /** Sent when the appointment is called off. Not required; see {@code NotificationObserver}. */
    CANCELLATION;

    public String label() {
        return switch (this) {
            case CONFIRMATION -> "Confirmation";
            case REMINDER -> "Reminder";
            case CANCELLATION -> "Cancellation";
        };
    }
}
