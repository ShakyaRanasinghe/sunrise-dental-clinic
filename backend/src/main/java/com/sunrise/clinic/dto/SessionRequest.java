package com.sunrise.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Request a receptionist submits to publish a dentist's availability window. */
public record SessionRequest(
        @NotBlank(message = "dentistId is required") String dentistId,
        @NotNull(message = "date is required") LocalDate date,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime,
        Integer slotMinutes) {

    public int slotMinutesOrDefault() {
        return slotMinutes == null ? 30 : slotMinutes;
    }
}
