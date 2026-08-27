package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;

import java.util.Optional;

/**
 * The confidentiality decision: <b>who may see an appointment's clinical detail</b>.
 *
 * <p>Only the treating dentist - the one this appointment is assigned to - or the patient
 * themselves. Never a receptionist, and never an administrator. That is not an oversight
 * about seniority: an administrator reads totals and audit trails, and a receptionist
 * books and bills, and neither needs to know why the patient came. Authority here
 * follows purpose, not rank.</p>
 *
 * <p>Note {@code canViewClinical} asks about <em>this</em> appointment, not about the
 * role. Any dentist holds {@code READ_CLINICAL}; only one of them is treating this
 * patient. A role check alone would let every dentist in the practice read every
 * diagnosis, which is the defect this class exists to prevent - and the same defect that
 * {@code complete} still had until this step, where {@code AccessControl.require(user,
 * Role.DENTIST)} let any dentist write a diagnosis into any other dentist's
 * appointment.</p>
 */
public class ClinicAccess {

    private final PatientRepository patients;
    private final DentistRepository dentists;

    public ClinicAccess(PatientRepository patients, DentistRepository dentists) {
        this.patients = patients;
        this.dentists = dentists;
    }

    /** @return true if {@code user} may see this appointment's diagnosis. */
    public boolean canViewClinical(Appointment appointment, ClinicPrincipal user) {
        if (user == null || appointment == null) {
            return false;
        }
        return switch (user.role()) {
            case DENTIST -> isTreatingDentist(appointment, user);
            case PATIENT -> isThePatient(appointment, user);
            // Reception books and bills; the administrator reads totals and the audit
            // trail. Neither needs the diagnosis.
            case RECEPTIONIST, ADMIN -> false;
        };
    }

    /**
     * @return true if {@code user} may read the patient's declared medical notes.
     *
     * <p>The same gate as the diagnosis, and deliberately one method rather than two
     * conditions written separately at two call sites (FR-NOTE-07, FR-NOTE-08). Notes
     * and diagnosis are both clinical detail about one person; a rule split in two is a
     * rule that can drift apart.</p>
     *
     * <p>The notes themselves arrive with the medical-notes work in a later step. This
     * gate is here now because it is the same decision, and answering it in one place is
     * the point.</p>
     */
    public boolean canViewPatientNotes(Appointment appointment, ClinicPrincipal user) {
        return canViewClinical(appointment, user);
    }

    /** @return the dentist record behind a signed-in dentist, if there is one. */
    public Optional<Dentist> dentistFor(ClinicPrincipal user) {
        return user == null ? Optional.empty() : dentists.findByUserUid(user.uid());
    }

    /** @return the patient record behind a signed-in patient, if there is one. */
    public Optional<Patient> patientFor(ClinicPrincipal user) {
        return user == null ? Optional.empty() : patients.findByUserUid(user.uid());
    }

    /** A patient by id, for naming them on a screen. */
    public Optional<Patient> patientById(String patientId) {
        return patientId == null ? Optional.empty() : patients.findById(patientId);
    }

    private boolean isTreatingDentist(Appointment appointment, ClinicPrincipal user) {
        return dentistFor(user).map(d -> appointment.isTreatedBy(d.getId())).orElse(false);
    }

    private boolean isThePatient(Appointment appointment, ClinicPrincipal user) {
        return patientFor(user).map(p -> appointment.belongsTo(p.getId())).orElse(false);
    }
}
