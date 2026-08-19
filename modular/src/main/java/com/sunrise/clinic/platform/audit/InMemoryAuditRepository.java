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

    @Override
    public java.util.List<AuditEvent> search(String actorUid, String targetId,
                                             java.time.LocalDate from, java.time.LocalDate to,
                                             int limit) {
        return store.values().stream()
                .filter(e -> blank(actorUid) || actorUid.trim().equals(e.getActorUid()))
                .filter(e -> blank(targetId) || targetId.trim().equals(e.getTargetId()))
                .filter(e -> from == null || !day(e).isBefore(from))
                .filter(e -> to == null || !day(e).isAfter(to))
                .sorted(java.util.Comparator.comparing(AuditEvent::getTimestamp).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static java.time.LocalDate day(AuditEvent event) {
        return event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
}
