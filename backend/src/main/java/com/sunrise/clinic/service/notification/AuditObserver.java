package com.sunrise.clinic.service.notification;

import com.sunrise.clinic.domain.AuditEvent;
import com.sunrise.clinic.repository.AuditRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Observer that writes an immutable audit record for every appointment event —
 * the append-only trail the Admin can review.
 */
public class AuditObserver implements AppointmentObserver {

    private final AuditRepository audit;

    public AuditObserver(AuditRepository audit) {
        this.audit = audit;
    }

    @Override
    public void onEvent(AppointmentEvent event) {
        audit.save(AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .actorUid(event.appointment().getCreatedByUid())
                .actorRole(event.appointment().getCreatedByRole())
                .action("APPOINTMENT_" + event.type())
                .targetType("Appointment")
                .targetId(event.appointment().getAppointmentNo())
                .timestamp(Instant.now())
                .build());
    }
}
