package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Treatment;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.TreatmentRepository;

import java.util.List;

/** In-memory {@link TreatmentRepository}. */
public class InMemoryTreatmentRepository
        extends InMemoryRepository<Treatment, String>
        implements TreatmentRepository {

    @Override
    protected String idOf(Treatment entity) {
        return entity.getId();
    }

    @Override
    public List<Treatment> findActive() {
        return store.values().stream().filter(Treatment::isActive).toList();
    }
}
