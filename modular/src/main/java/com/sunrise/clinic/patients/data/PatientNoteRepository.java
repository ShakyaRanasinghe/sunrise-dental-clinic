package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.PatientNote;
import com.sunrise.clinic.platform.data.Repository;

import java.util.List;

/** Persistence for {@link PatientNote}s. */
public interface PatientNoteRepository extends Repository<PatientNote, String> {

    /**
     * Every note a patient has declared, <b>critical first</b>.
     *
     * <p>The order is part of the contract, not a convenience. FR-NOTE-08 requires a dentist
     * to see a critical note before treating, and a list that puts the allergy third is a
     * list that gets skimmed past.</p>
     */
    List<PatientNote> findByPatientId(String patientId);

    /** Whether this patient has declared anything critical - for the schedule banner. */
    boolean hasCritical(String patientId);
}
