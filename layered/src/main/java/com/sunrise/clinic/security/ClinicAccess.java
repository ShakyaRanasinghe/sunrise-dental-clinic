package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.repository.DentistRepository;
import com.sunrise.clinic.repository.PatientRepository;

/**
 * Centralises the confidentiality decision: <b>who may see an appointment's clinical
 * diagnosis</b>. Only the treating dentist (their own patient) or the patient themselves —
 * never Admin or Receptionist. Callers use this to choose which DTO to return.
 */
public class ClinicAccess {

    private final PatientRepository patients;
    private final DentistRepository dentists;

    public ClinicAccess(PatientRepository patients, DentistRepository dentists) {
        this.patients = patients;
        this.dentists = dentists;
    }

    public boolean canViewClinical(Appointment appointment, ClinicPrincipal user) {
        if (user == null) {
            return false;
        }
        return switch (user.role()) {
            case DENTIST -> dentists.findByUserUid(user.uid())
                    .map(d -> d.getId().equals(appointment.getDentistId()))
                    .orElse(false);
            case PATIENT -> patients.findByUserUid(user.uid())
                    .map(p -> p.getId().equals(appointment.getPatientId()))
                    .orElse(false);
            // Admin and Receptionist never see clinical diagnosis.
            default -> false;
        };
    }
}
