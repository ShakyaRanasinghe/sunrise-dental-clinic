package com.sunrise.clinic.notifications.domain;

/**
 * What became of a send attempt - FR-NOT-03.
 *
 * <p>Three outcomes, and the third is the one worth explaining.</p>
 */
public enum NotificationStatus {

    /** The channel accepted it. */
    SENT,
    /**
     * The channel refused or failed.
     *
     * <p>Recorded and then forgotten: FR-NOT-04 says a delivery failure must never fail the
     * operation that triggered it, so a booking whose confirmation could not be sent is still a
     * booking. The row is how anybody finds out afterwards.</p>
     */
    FAILED,
    /**
     * Written down rather than sent, because no real channel is configured.
     *
     * <p>The honest state for a development machine and for a clinic that has not set up a mail
     * relay: the message is recorded exactly as it would have been sent, and nothing goes out.
     * It is a distinct value rather than being folded into SENT, because "we have no mail
     * server" and "the patient was told" are different facts and a report that conflated them
     * would be lying about whether anybody was informed.</p>
     */
    LOGGED;

    /** @return true if the patient can be assumed to have been told. */
    public boolean reachedTheRecipient() {
        return this == SENT;
    }
}
