package com.sunrise.clinic.dto;

import com.sunrise.clinic.domain.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/** A bookable slot as shown to a patient browsing availability. */
public record SlotResponse(
        String id,
        String dentistId,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        SlotStatus status) {
}
