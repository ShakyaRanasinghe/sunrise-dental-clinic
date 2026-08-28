package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.AuditEvent;

/** Append-only persistence for {@link AuditEvent}s. */
public interface AuditRepository extends Repository<AuditEvent, String> {
}
