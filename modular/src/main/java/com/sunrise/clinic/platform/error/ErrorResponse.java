package com.sunrise.clinic.platform.error;

/**
 * A safe, structured error body. Carries a stable machine-readable {@code errorCode}
 * for the client to branch on and a human message — never a stack trace or internal detail.
 */
public record ErrorResponse(String errorCode, String message) {
}
