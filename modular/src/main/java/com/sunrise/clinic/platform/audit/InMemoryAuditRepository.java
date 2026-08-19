package com.sunrise.clinic.platform.audit;

import com.sunrise.clinic.platform.data.InMemoryRepository;

/** In-memory {@link AuditRepository}. */
public class InMemoryAuditRepository
        extends InMemoryRepository<AuditEvent, String>
        implements AuditRepository {

    @Override
    protected String idOf(AuditEvent entity) {
        return entity.getId();
    }
}
