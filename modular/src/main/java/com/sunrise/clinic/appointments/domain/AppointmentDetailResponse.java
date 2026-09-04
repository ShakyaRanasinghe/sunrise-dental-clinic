package com.sunrise.clinic.appointments.domain;

import com.sunrise.clinic.patients.domain.PatientNoteResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * An appointment including its clinical detail, served <b>only</b> to the treating
 * dentist or to the patient themselves - the choice made by {@code ClinicAccess}.
 *
 * <p>It carries the patient's declared medical notes, because this is the only response a
 * dentist receives and FR-NOTE-07 wants the notes beside the appointment. {@link
 * AppointmentResponse} - what a receptionist and an administrator get - has no field for
 * them, which is how FR-NOTE-11 is enforced by the type rather than by a screen remembering
 * not to print something.</p>
 *
 * <p>A separate record from {@link AppointmentResponse} rather than one record with an
 * optional field. The two differ in who may hold them, so making them different types
 * means the compiler carries the distinction: a route that returns the wrong one is a
 * visible decision in the code, not a null check that happened to go the wrong way.</p>
 */
public record AppointmentDetailResponse(String appointmentNo,
                                        String patientId,
                                        String patientName,
                                        String dentistId,
                                        String dentistName,
                                        String slotId,
                                        String treatmentId,
                                        String treatmentName,
                                        // GAP-REC-13: the patient's stated reason for an "Other"
                                        // booking — non-clinical (reception already takes it at
                                        // booking), so desk screens may print it as the
                                        // treatment fallback.
                                        String patientReason,
                                        LocalDate date,
                                        LocalTime time,
                                        AppointmentStatus status,
                                        String diagnosis,
                                        List<PatientNoteResponse> patientNotes,
                                        boolean hasCriticalNotes) {

    /** Without notes - for a caller that may see the diagnosis but has no notes to hand. */
    public static AppointmentDetailResponse of(Appointment appointment, String patientName,
                                               String dentistName, String treatmentName) {
        return of(appointment, patientName, dentistName, treatmentName, List.of());
    }

    /**
     * With the patient's declared notes - FR-NOTE-07.
     *
     * <p>Alongside the appointment rather than behind a separate request, because a dentist
     * about to treat someone should not have to know to go and look.</p>
     *
     * @param patientNotes may be empty, and empty is a real answer: the screen must say
     *                     "nothing declared" rather than leave a blank (FR-NOTE-12)
     */
    public static AppointmentDetailResponse of(Appointment appointment, String patientName,
                                               String dentistName, String treatmentName,
                                               List<PatientNoteResponse> patientNotes) {
        List<PatientNoteResponse> notes = patientNotes == null ? List.of() : patientNotes;
        return new AppointmentDetailResponse(
                appointment.getAppointmentNo(),
                appointment.getPatientId(), patientName,
                appointment.getDentistId(), dentistName,
                appointment.getSlotId(),
                appointment.getTreatmentId(), treatmentName,
                appointment.getPatientReason(),
                appointment.getDate(), appointment.getTime(),
                appointment.getStatus(),
                appointment.getDiagnosis(),
                notes,
                notes.stream().anyMatch(PatientNoteResponse::critical));
    }

    /** @return true when the patient has declared nothing - the explicit empty state. */
    public boolean hasNoNotes() {
        return patientNotes.isEmpty();
    }

    public boolean isCancellable() {
        return status != null && status.canMoveTo(AppointmentStatus.CANCELLED);
    }
}
