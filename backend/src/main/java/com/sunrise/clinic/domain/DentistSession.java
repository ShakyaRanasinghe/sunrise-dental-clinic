package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * An availability window a Receptionist publishes for a dentist —
 * "Dr. Silva is in the clinic on 2026-07-20 from 16:00 to 18:00".
 * The {@code SlotService} explodes it into bookable {@link Slot}s.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DentistSession {
    private String id;
    private String dentistId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    @Builder.Default
    private int slotDurationMinutes = 30;
    private String publishedByUid;   // receptionist/admin
}
