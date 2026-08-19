package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.platform.audit.AuditEvent;
import com.sunrise.clinic.platform.audit.AuditRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Observer that writes an immutable audit record for every appointment event - the
 * append-only trail an administrator can review (FR-ADM-30).
 *
 * <p>Failures here must not fail the booking: the appointment is already committed by
 * the time this runs, and refusing it because the trail could not be written would
 * lose a real appointment over a bookkeeping problem. {@link AppointmentEventPublisher}
 * isolates each observer for that reason.</p>
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
                // The actor, not the booker - see AppointmentEvent.
                .actorUid(event.actorUid())
                .actorRole(event.actorRole())
                .action("APPOINTMENT_" + event.type())
                .targetType("Appointment")
                .targetId(event.appointment().getAppointmentNo())
                .timestamp(Instant.now())
                .build());
    }
}
