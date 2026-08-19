package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;

/**
 * Immutable, non-throwing result of a notification send attempt.
 * A stable machine-readable {@code reason} lets callers branch and lets us
 * record analytics, without ever throwing out of a best-effort notification.
 *
 * @param sent    whether the message left the system (or was accepted/logged)
 * @param reason  stable code: {@code sent} | {@code logged} | {@code send_failed} | ...
 * @param detail  human-readable detail (for logs; never shown raw to a user)
 * @param channel which channel handled it
 */
public record DispatchResult(boolean sent, String reason, String detail, ChannelType channel) {
}
