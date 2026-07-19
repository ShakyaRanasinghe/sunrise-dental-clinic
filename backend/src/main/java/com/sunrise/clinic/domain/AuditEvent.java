package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** An immutable, append-only record of an action taken in the system (audit trail). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String id;
    private String actorUid;
    private Role actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private Instant timestamp;
}
