package com.sunrise.clinic.feedback.domain;

import java.util.Set;

/**
 * A complaint's progress - FR-CMP-05.
 *
 * <pre>
 *   SUBMITTED ──→ UNDER_REVIEW ──→ RESOLVED
 *                              └──→ DISMISSED
 * </pre>
 *
 * <p>The rules live here for the same reason {@code AppointmentStatus}'s do: a check in a
 * service is one the next service forgets. A complaint cannot go straight from SUBMITTED to
 * RESOLVED - somebody has to have looked at it, and recording that they did is the
 * difference between a process and a filing cabinet.</p>
 */
public enum ComplaintStatus {

    /** Raised by the patient, not yet picked up. */
    SUBMITTED,
    /** An administrator is looking at it. */
    UNDER_REVIEW,
    /** Closed, with a written resolution. Terminal. */
    RESOLVED,
    /** Closed without action, with a written reason. Terminal. */
    DISMISSED;

    public boolean canMoveTo(ComplaintStatus next) {
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW;
            case UNDER_REVIEW -> next == RESOLVED || next == DISMISSED;
            case RESOLVED, DISMISSED -> false;
        };
    }

    /** @return true if the complaint is closed and nothing further can happen. */
    public boolean isClosed() {
        return this == RESOLVED || this == DISMISSED;
    }

    /** @return true if it needs an administrator's attention - FR-ADM-51. */
    public boolean isOpen() {
        return !isClosed();
    }

    /** @return true if closing into this state needs a written explanation - FR-ADM-53. */
    public boolean requiresResolution() {
        return isClosed();
    }

    public String label() {
        return switch (this) {
            case SUBMITTED -> "Received";
            case UNDER_REVIEW -> "Being looked at";
            case RESOLVED -> "Resolved";
            case DISMISSED -> "Closed";
        };
    }

    /** Everything an administrator may filter by, plus the two groupings they think in. */
    public static Set<ComplaintStatus> open() {
        return Set.of(SUBMITTED, UNDER_REVIEW);
    }
}
