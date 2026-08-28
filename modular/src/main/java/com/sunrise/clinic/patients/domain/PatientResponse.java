package com.sunrise.clinic.patients.domain;

import java.time.LocalDate;

/**
 * What a patient record looks like once it leaves the module.
 *
 * <p>Introduced to close a defect rather than to add a layer. {@code Patient}
 * overrides {@code toString()} as {@code "Patient{id=…}"}, and the previous JSON
 * writer fell back to {@code toString()} for anything it did not recognise, so
 * {@code GET /api/patients} answered:</p>
 *
 * <pre>["Patient{id=p-nimal}", "Patient{id=p-kamala}"]</pre>
 *
 * <p>The writer now handles beans and records, so the defect is fixed either way -
 * but serialising {@code Patient} directly would publish {@code userUid}, and an
 * internal account identifier on a screen invites its use as a parameter. This
 * record carries the seven fields a caller needs and not that one.</p>
 *
 * <p>{@code hasPortalAccount} is the one derived field: reception needs to know
 * whether a patient can self-serve (FR-REC-22) without being handed the uid.</p>
 */
public record PatientResponse(String id,
                              String name,
                              String address,
                              String contactNumber,
                              String email,
                              LocalDate dob,
                              boolean hasPortalAccount) {

    /** @return the outward form of {@code patient}. */
    public static PatientResponse of(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getAddress(),
                patient.getContactNumber(),
                patient.getEmail(),
                patient.getDob(),
                patient.getUserUid() != null);
    }
}
