package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.InMemoryRepository;

import com.sunrise.clinic.scheduling.domain.Dentist;

import java.util.List;
import java.util.Optional;

/** In-memory {@link DentistRepository}. */
public class InMemoryDentistRepository
        extends InMemoryRepository<Dentist, String>
        implements DentistRepository {

    @Override
    protected String idOf(Dentist entity) {
        return entity.getId();
    }

    @Override
    public Optional<Dentist> findByUserUid(String userUid) {
        return store.values().stream()
                .filter(d -> userUid != null && userUid.equals(d.getUserUid()))
                .findFirst();
    }

    @Override
    public List<Dentist> findActive() {
        return store.values().stream()
                .filter(Dentist::isActive)
                .sorted(java.util.Comparator.comparing(Dentist::getName,
                        java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
