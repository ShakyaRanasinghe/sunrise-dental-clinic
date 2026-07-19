package com.sunrise.clinic.dto;

import com.sunrise.clinic.domain.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Clinical appointment view — includes the confidential {@code diagnosis}. Served ONLY to
 * the treating Dentist or the Patient themselves (decided by {@code ClinicAccess}). A
 * separate class from {@link AppointmentResponse} so the diagnosis is impossible to expose
 * to the wrong role by accident.
 */
public record AppointmentDetailResponse(
        String appointmentNo,
        String patientId,
        String dentistId,
        String slotId,
        String treatmentId,
        LocalDate date,
        LocalTime time,
        AppointmentStatus status,
        String diagnosis) {
}
