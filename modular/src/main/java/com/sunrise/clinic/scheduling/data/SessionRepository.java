package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.scheduling.domain.DentistSession;

import java.time.LocalDate;
import java.util.List;

/** Persistence for {@link DentistSession}s (published availability windows). */
public interface SessionRepository extends Repository<DentistSession, String> {

    List<DentistSession> findByDentistId(String dentistId);

    List<DentistSession> findByDate(LocalDate date);

    /** All sessions on or after {@code from}, ordered by date then start time. */
    List<DentistSession> findFromDate(LocalDate from);
}
