package com.sunrise.clinic.appointments.data;

import com.sunrise.clinic.platform.data.InMemoryRepository;

import com.sunrise.clinic.appointments.domain.Appointment;

import java.time.LocalDate;
import java.util.List;

/** In-memory {@link AppointmentRepository} (default; used for tests + offline demo). */
public class InMemoryAppointmentRepository
        extends InMemoryRepository<Appointment, String>
        implements AppointmentRepository {

    @Override
    protected String idOf(Appointment entity) {
        return entity.getAppointmentNo();
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        return store.values().stream().filter(a -> patientId.equals(a.getPatientId())).toList();
    }

    @Override
    public List<Appointment> findByDentistId(String dentistId) {
        return store.values().stream().filter(a -> dentistId.equals(a.getDentistId())).toList();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return store.values().stream().filter(a -> date.equals(a.getDate())).toList();
    }

    @Override
    public List<Appointment> findByDateBetween(LocalDate from, LocalDate to) {
        return store.values().stream()
                .filter(a -> a.getDate() != null
                        && !a.getDate().isBefore(from) && !a.getDate().isAfter(to))
                .toList();
    }
}
