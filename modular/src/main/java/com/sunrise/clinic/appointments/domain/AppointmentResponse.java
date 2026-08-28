package com.sunrise.clinic.appointments.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * An appointment as reception and the administrator see it.
 *
 * <p><b>It has no diagnosis field, and that is the design.</b> Clinical detail cannot
 * leak through this record because there is nothing to leak it through - no mapper to
 * remember to strip it, no {@code if} to get backwards. The treating dentist and the
 * patient get {@link AppointmentDetailResponse} instead, chosen by
 * {@code ClinicAccess}.</p>
 *
 * <p>Carries the patient's and dentist's <em>names</em> as well as their ids. The
 * previous version carried ids only, so the day view showed
 * {@code p-nimal} where it needed "Nimal Perera" - the same defect as
 * {@code SlotResponse}, and the reason every dashboard needed a second lookup per row.</p>
 */
public record AppointmentResponse(String appointmentNo,
                                  String patientId,
                                  String patientName,
                                  String dentistId,
                                  String dentistName,
                                  String slotId,
                                  String treatmentId,
                                  String treatmentName,
                                  LocalDate date,
                                  LocalTime time,
                                  AppointmentStatus status) {

    public static AppointmentResponse of(Appointment appointment, String patientName,
                                         String dentistName, String treatmentName) {
        return new AppointmentResponse(
                appointment.getAppointmentNo(),
                appointment.getPatientId(), patientName,
                appointment.getDentistId(), dentistName,
                appointment.getSlotId(),
                appointment.getTreatmentId(), treatmentName,
                appointment.getDate(), appointment.getTime(),
                appointment.getStatus());
    }

    /** @return true if this appointment can still be cancelled by whoever holds it. */
    public boolean isCancellable() {
        return status != null && status.canMoveTo(AppointmentStatus.CANCELLED);
    }
}
