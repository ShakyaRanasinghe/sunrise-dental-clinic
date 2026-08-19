package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.AuditEvent;
import com.sunrise.clinic.repository.AuditRepository;
import com.sunrise.clinic.repository.InMemoryRepository;

/** In-memory {@link AuditRepository}. */
public class InMemoryAuditRepository
        extends InMemoryRepository<AuditEvent, String>
        implements AuditRepository {

    @Override
    protected String idOf(AuditEvent entity) {
        return entity.getId();
    }
}
