package com.sunrise.clinic.appointments.domain;

import java.util.Set;

/**
 * The appointment lifecycle, and the only place that says which move is legal.
 *
 * <pre>
 *   CONFIRMED ──→ COMPLETED ──→ BILLED
 *       │              │
 *       └──────────────┴──→ CANCELLED
 * </pre>
 *
 * <p>In {@code layered/} this was a bare enum of four names and the rules lived
 * nowhere: {@code complete()} assigned COMPLETED whatever the current status was, and
 * {@code cancel()} assigned CANCELLED the same way. So a cancelled appointment could
 * be completed, and - the one that matters financially - a <b>BILLED</b> appointment
 * could be cancelled, releasing its slot while the bill stood.</p>
 *
 * <p>Putting the rule on the enum means every caller gets it. A check in the service
 * would be one a second service could forget, and a check in the servlet would be one
 * the next servlet could forget.</p>
 */
public enum AppointmentStatus {

    /** Booked and expected. */
    CONFIRMED,
    /** Treated, with a diagnosis recorded. Ready to bill. */
    COMPLETED,
    /** Billed. Terminal: the money has been accounted for. */
    BILLED,
    /** Called off. Terminal, and the slot has been released. */
    CANCELLED;

    /**
     * @return true if an appointment in this status may move to {@code next}
     */
    public boolean canMoveTo(AppointmentStatus next) {
        return permitted().contains(next);
    }

    /** @return true if nothing may follow this status. */
    public boolean isTerminal() {
        return permitted().isEmpty();
    }

    /**
     * @return true if the appointment still occupies its slot. CANCELLED released it;
     *         BILLED and COMPLETED refer to a visit that happened, so the slot stays
     *         spoken for
     */
    public boolean holdsSlot() {
        return this != CANCELLED;
    }

    private Set<AppointmentStatus> permitted() {
        return switch (this) {
            case CONFIRMED -> Set.of(COMPLETED, CANCELLED);
            // A visit that happened can still be called off before it is billed -
            // a patient who never turned up, recorded in error.
            case COMPLETED -> Set.of(BILLED, CANCELLED);
            case BILLED, CANCELLED -> Set.of();
        };
    }
}
