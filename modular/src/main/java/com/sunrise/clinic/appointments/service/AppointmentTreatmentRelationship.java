package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.appointments.data.AppointmentRepository;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.patients.service.TreatmentRelationship;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;

/**
 * The appointments module's answer to the question the patients module asks.
 *
 * <p>Implements {@link TreatmentRelationship}, declared in {@code patients} - so the
 * dependency runs appointments → patients, the direction it already ran, and no cycle
 * appears. See that interface for why the inversion is there.</p>
 *
 * <p>"Treating" means <b>any</b> appointment, including a cancelled one. A dentist who was
 * booked to see a patient next week and had it cancelled still had a clinical reason to read
 * that patient's declared allergies, and a rule that revoked the reading at the moment of
 * cancellation would take the notes away mid-consultation if the appointment were called off
 * while the dentist had the screen open. The narrower rule buys nothing: this is about
 * whether a clinical relationship exists at all, not about a particular visit.</p>
 */
public class AppointmentTreatmentRelationship implements TreatmentRelationship {

    private final AppointmentRepository appointments;
    private final DentistRepository dentists;

    public AppointmentTreatmentRelationship(AppointmentRepository appointments,
                                            DentistRepository dentists) {
        this.appointments = appointments;
        this.dentists = dentists;
    }

    @Override
    public boolean isTreating(String dentistUid, String patientId) {
        if (dentistUid == null || patientId == null) {
            return false;
        }
        return dentists.findByUserUid(dentistUid)
                .map(Dentist::getId)
                .map(dentistId -> appointments.findByDentistId(dentistId).stream()
                        .anyMatch(appointment -> appointment.belongsTo(patientId)))
                .orElse(false);
    }
}
