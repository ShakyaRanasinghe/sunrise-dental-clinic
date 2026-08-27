package com.sunrise.clinic.scheduling.domain;

/** Availability slot state. A slot may be booked exactly once (double-booking guard). */
public enum SlotStatus {
    OPEN,
    BOOKED
}
