package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The central aggregate. {@code appointmentNo} (APT-yyyymmdd-####) is generated
 * by the Singleton {@code AppointmentNumberGenerator}.
 *
 * <p><b>Confidentiality:</b> {@code diagnosis} is clinical data — visible only to the
 * treating Dentist and the Patient. It is stripped from responses served to
 * Receptionist/Admin by the DTO mapper.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private String appointmentNo;      // primary key, e.g. APT-20260720-0001
    private String patientId;
    private String dentistId;
    private String slotId;
    private String treatmentId;
    private LocalDate date;
    private LocalTime time;
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    /** Clinical diagnosis — CONFIDENTIAL (dentist + patient only). */
    private String diagnosis;

    private String createdByUid;
    private Role createdByRole;
    private Instant createdAt;
}
