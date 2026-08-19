package com.sunrise.clinic.platform.audit;

import com.sunrise.clinic.platform.data.Repository;

/** Append-only persistence for {@link AuditEvent}s. */
public interface AuditRepository extends Repository<AuditEvent, String> {
}
