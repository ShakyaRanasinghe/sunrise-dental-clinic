package com.sunrise.clinic.patients.domain;

import java.time.Instant;

/**
 * A declared note as it leaves the module.
 *
 * <p>Reaching a screen at all means the caller has already been checked - only the patient
 * themselves or a dentist treating them can obtain one of these (FR-NOTE-09, FR-NOTE-11).
 * There is no "redacted" variant, because a receptionist does not get a redacted note; they
 * get nothing.</p>
 */
public record PatientNoteResponse(String id,
                                  NoteCategory category,
                                  String categoryLabel,
                                  String detail,
                                  boolean critical,
                                  Instant updatedAt) {

    public static PatientNoteResponse of(PatientNote note) {
        return new PatientNoteResponse(note.getId(), note.getCategory(),
                note.getCategory() == null ? "" : note.getCategory().label(),
                note.getDetail(), note.isCritical(),
                note.getUpdatedAt() == null ? note.getCreatedAt() : note.getUpdatedAt());
    }
}
