package com.sunrise.clinic.scheduling.domain;

import java.math.BigDecimal;

/**
 * A dentist as the booking screens and {@code GET /api/dentists} see them.
 *
 * <p>Together with {@link TreatmentResponse} and {@link SlotResponse} this closes the
 * {@code toString()} defect. {@code GET /api/dentists} used to answer</p>
 *
 * <pre>["Dentist{id=d-silva}", "Dentist{id=d-jayasuriya}"]</pre>
 *
 * <p>because the web tier serialised the entity directly and the JSON writer fell
 * back to {@code toString()} for types it did not recognise. A patient choosing a
 * dentist was shown an identifier and nothing else - no name, no specialisation, no
 * fee.</p>
 *
 * <p>{@code userUid} is omitted deliberately, for the same reason it is omitted from
 * {@code PatientResponse}: it is an internal account identifier and a screen has no
 * use for it.</p>
 */
public record DentistResponse(String id,
                              String name,
                              String specialization,
                              BigDecimal consultationFee,
                              boolean active) {

    public static DentistResponse of(Dentist dentist) {
        return new DentistResponse(
                dentist.getId(),
                dentist.getName(),
                dentist.getSpecialization(),
                dentist.getConsultationFee(),
                dentist.isActive());
    }
}
