package com.sunrise.clinic.platform.audit;

import com.sunrise.clinic.platform.data.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Append-only persistence for {@link AuditEvent}s.
 *
 * <p>There is deliberately no update and no delete beyond what {@link Repository}
 * declares, and nothing in the application calls those - FR-ADM-31. A trail that can be
 * edited is not a trail.</p>
 */
public interface AuditRepository extends Repository<AuditEvent, String> {

    /**
     * The trail, newest first, narrowed by whatever the caller supplied - FR-ADM-30.
     *
     * <p>Every filter is optional. Passing none returns the most recent {@code limit}
     * records, which is what the screen shows when it opens.</p>
     *
     * @param actorUid  only this actor, or null for anyone
     * @param targetId  only this target - an appointment number, an account uid - or null.
     *                  This is what answers "who changed this appointment, and when"
     *                  (FR-ADM-32)
     * @param from      earliest day to include, or null
     * @param to        latest day to include, or null
     * @param limit     how many records at most, so one screen cannot pull a year
     */
    List<AuditEvent> search(String actorUid, String targetId,
                            LocalDate from, LocalDate to, int limit);
}
