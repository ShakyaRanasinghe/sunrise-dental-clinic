package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.DentistSession;

import java.time.LocalDate;
import java.util.List;

/** Persistence for {@link DentistSession}s (published availability windows). */
public interface SessionRepository extends Repository<DentistSession, String> {

    List<DentistSession> findByDentistId(String dentistId);

    List<DentistSession> findByDate(LocalDate date);
}
