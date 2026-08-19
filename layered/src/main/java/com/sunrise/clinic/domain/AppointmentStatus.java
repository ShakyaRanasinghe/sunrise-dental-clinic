package com.sunrise.clinic.domain;

/**
 * Appointment lifecycle:
 * CONFIRMED &rarr; COMPLETED &rarr; BILLED, with CANCELLED as an exit state.
 */
public enum AppointmentStatus {
    CONFIRMED,
    COMPLETED,
    BILLED,
    CANCELLED
}
