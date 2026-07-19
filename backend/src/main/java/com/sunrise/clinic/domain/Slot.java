package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/** A single bookable time slot. Booking flips OPEN &rarr; BOOKED atomically. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slot {
    private String id;
    private String sessionId;
    private String dentistId;
    private LocalDate date;
    private LocalTime startTime;
    @Builder.Default
    private int durationMinutes = 30;
    @Builder.Default
    private SlotStatus status = SlotStatus.OPEN;
    private String appointmentNo;    // set when booked
}
