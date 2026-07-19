package com.sunrise.clinic.domain;

/** Availability slot state. A slot may be booked exactly once (double-booking guard). */
public enum SlotStatus {
    OPEN,
    BOOKED
}
