package com.sunrise.clinic.scheduling.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A bookable slot as shown to a patient browsing availability.
 *
 * <p>Carries the dentist's name as well as the id. In {@code layered/} it carried
 * only {@code dentistId}, so a screen listing this week's availability across every
 * dentist had no way to say whose slot each one was without a second lookup per
 * row.</p>
 */
public record SlotResponse(
        String id,
        String dentistId,
        String dentistName,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        SlotStatus status) {

    /** @param dentistName resolved by the service; the slot row holds only the id. */
    public static SlotResponse of(Slot slot, String dentistName) {
        return new SlotResponse(
                slot.getId(),
                slot.getDentistId(),
                dentistName,
                slot.getDate(),
                slot.getStartTime(),
                slot.getDurationMinutes(),
                slot.getStatus());
    }
}
