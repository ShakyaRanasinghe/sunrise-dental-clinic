package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.InMemoryRepository;

import com.sunrise.clinic.scheduling.domain.DentistSession;

import java.time.LocalDate;
import java.util.List;

/** In-memory {@link SessionRepository}. */
public class InMemorySessionRepository
        extends InMemoryRepository<DentistSession, String>
        implements SessionRepository {

    @Override
    protected String idOf(DentistSession entity) {
        return entity.getId();
    }

    @Override
    public List<DentistSession> findByDentistId(String dentistId) {
        return store.values().stream().filter(s -> dentistId.equals(s.getDentistId())).toList();
    }

    @Override
    public List<DentistSession> findByDate(LocalDate date) {
        return store.values().stream().filter(s -> date.equals(s.getDate())).toList();
    }

    @Override
    public List<DentistSession> findFromDate(LocalDate from) {
        return store.values().stream()
                .filter(s -> !s.getDate().isBefore(from))
                .sorted(java.util.Comparator.comparing(DentistSession::getDate)
                        .thenComparing(DentistSession::getStartTime))
                .toList();
    }
}
