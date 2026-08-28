package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.DentistSession;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.SessionRepository;

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
}
