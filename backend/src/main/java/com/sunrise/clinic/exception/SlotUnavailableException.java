package com.sunrise.clinic.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a booking is attempted against a slot that is not OPEN.
 * Maps to HTTP 409 Conflict — the guard against double-booking.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
