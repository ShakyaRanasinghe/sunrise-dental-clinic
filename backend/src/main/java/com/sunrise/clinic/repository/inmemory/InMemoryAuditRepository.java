package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.AuditEvent;
import com.sunrise.clinic.repository.AuditRepository;
import com.sunrise.clinic.repository.InMemoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** In-memory {@link AuditRepository}. */
@Repository
@Profile("!firestore")
public class InMemoryAuditRepository
        extends InMemoryRepository<AuditEvent, String>
        implements AuditRepository {

    @Override
    protected String idOf(AuditEvent entity) {
        return entity.getId();
    }
}
