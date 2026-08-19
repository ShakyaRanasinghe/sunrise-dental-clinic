package com.sunrise.clinic.patients.service;

/**
 * Whether a dentist is treating a patient.
 *
 * <p><b>Declared here and implemented elsewhere</b>, which is the point. FR-NOTE-09 says a
 * dentist may read notes only for patients on their own schedule - so the notes service has
 * to ask a question only the appointments module can answer. Having {@code patients} import
 * {@code appointments} would invert the dependency order the whole structure rests on:
 * appointments already depends on patients, and a cycle between two modules is the thing
 * that makes a codebase impossible to reason about one module at a time.</p>
 *
 * <p>So the module that needs the answer declares the question, and the module that has the
 * data implements it. {@code AppContext} connects the two. This is the only place in the
 * application where that inversion was necessary, and it is necessary because the
 * confidentiality rule genuinely spans both.</p>
 */
public interface TreatmentRelationship {

    /**
     * @param dentistUid the signed-in dentist's account uid, not their dentist id - the
     *                   caller has a principal, not a profile
     * @param patientId  the patient whose notes are being asked for
     * @return true if this dentist has an appointment with this patient
     */
    boolean isTreating(String dentistUid, String patientId);

    /** Nobody is treating anybody. For tests that do not care. */
    TreatmentRelationship NONE = (dentistUid, patientId) -> false;
}
