package com.sunrise.clinic.exception;

/**
 * Thrown when a booking is attempted against a slot that is not OPEN.
 * The web layer maps it to HTTP 409 Conflict — the guard against double-booking.
 */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
