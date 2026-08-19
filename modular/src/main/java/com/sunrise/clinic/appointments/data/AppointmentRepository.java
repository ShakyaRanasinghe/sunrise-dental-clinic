package com.sunrise.clinic.appointments.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.appointments.domain.Appointment;

import java.time.LocalDate;
import java.util.List;

/** Persistence for {@link Appointment} (keyed by appointmentNo). */
public interface AppointmentRepository extends Repository<Appointment, String> {

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByDentistId(String dentistId);

    List<Appointment> findByDate(LocalDate date);

    List<Appointment> findByDateBetween(LocalDate from, LocalDate to);
}
