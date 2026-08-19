package com.sunrise.clinic.appointments.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * An appointment including its clinical detail, served <b>only</b> to the treating
 * dentist or to the patient themselves - the choice made by {@code ClinicAccess}.
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
                                        LocalDate date,
                                        LocalTime time,
                                        AppointmentStatus status,
                                        String diagnosis) {

    public static AppointmentDetailResponse of(Appointment appointment, String patientName,
                                               String dentistName, String treatmentName) {
        return new AppointmentDetailResponse(
                appointment.getAppointmentNo(),
                appointment.getPatientId(), patientName,
                appointment.getDentistId(), dentistName,
                appointment.getSlotId(),
                appointment.getTreatmentId(), treatmentName,
                appointment.getDate(), appointment.getTime(),
                appointment.getStatus(),
                appointment.getDiagnosis());
    }

    public boolean isCancellable() {
        return status != null && status.canMoveTo(AppointmentStatus.CANCELLED);
    }
}
