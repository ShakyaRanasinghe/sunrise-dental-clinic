package com.sunrise.clinic.scheduling.domain;

import java.math.BigDecimal;

/**
 * A treatment as the booking screens and {@code GET /api/treatments} see them.
 *
 * <p>{@code baseCost} is the published price before the consultation fee and any
 * discount - what {@code fn_calculate_bill} starts from. It is shown to the patient
 * at booking so the cost is known before the appointment, not after it.</p>
 */
public record TreatmentResponse(String id,
                                String name,
                                String description,
                                BigDecimal baseCost,
                                boolean active) {

    public static TreatmentResponse of(Treatment treatment) {
        return new TreatmentResponse(
                treatment.getId(),
                treatment.getName(),
                treatment.getDescription(),
                treatment.getBaseCost(),
                treatment.isActive());
    }
}
