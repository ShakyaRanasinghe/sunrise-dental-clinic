package com.sunrise.clinic.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to book an appointment. {@code patientId} is optional — when a patient books
 * for themselves it is taken from the authenticated user; a receptionist supplies it.
 */
public record BookingRequest(
        @NotBlank(message = "slotId is required") String slotId,
        @NotBlank(message = "treatmentId is required") String treatmentId,
        String patientId) {
}
