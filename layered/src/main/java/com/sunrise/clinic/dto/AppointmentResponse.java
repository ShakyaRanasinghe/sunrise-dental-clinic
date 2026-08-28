package com.sunrise.clinic.dto;

import com.sunrise.clinic.domain.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Base appointment view — served to Receptionist and Admin.
 * <p><b>Deliberately has no diagnosis field.</b> Clinical data cannot leak through this
 * DTO because it simply does not exist on the class. Dentists/patients get the richer
 * {@link AppointmentDetailResponse} instead.</p>
 */
public record AppointmentResponse(
        String appointmentNo,
        String patientId,
        String dentistId,
        String slotId,
        String treatmentId,
        LocalDate date,
        LocalTime time,
        AppointmentStatus status) {
}
