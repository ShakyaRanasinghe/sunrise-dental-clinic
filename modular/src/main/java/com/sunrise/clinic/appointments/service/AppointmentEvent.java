package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.appointments.domain.Appointment;

/**
 * A domain event carried by the Observer pattern. Published when an appointment changes
 * state; observers react - write the audit record, send the confirmation - without
 * {@link AppointmentService} knowing who is listening.
 *
 * <p><b>The event carries the actor.</b> It did not, and {@link AuditObserver} fell back
 * to {@code appointment.createdByUid} - so cancelling an appointment was recorded in the
 * audit trail as having been done by whoever <em>booked</em> it. An administrator
 * cancelling a patient's appointment appeared in the trail as the patient. The trail
 * exists for accountability, so the one field it cannot get wrong is who acted.</p>
 *
 * @param type           what happened
 * @param appointment    the affected appointment
 * @param actorUid       who caused it - not necessarily who booked it
 * @param actorRole      the actor's role, as a name. A snapshot: the trail should still
 *                       read correctly after the account's role is changed
 * @param recipientEmail the patient's email for the confirmation, may be null
 * @param recipientName  the patient's display name
 */
public record AppointmentEvent(
        Type type,
        Appointment appointment,
        String actorUid,
        String actorRole,
        String recipientEmail,
        String recipientName) {

    public enum Type {
        CREATED,
        CANCELLED,
        COMPLETED,
        BILLED
    }

    /** @param actor the signed-in user who caused this. */
    public static AppointmentEvent of(Type type, Appointment appointment, ClinicPrincipal actor,
                                      String recipientEmail, String recipientName) {
        return new AppointmentEvent(type, appointment,
                actor == null ? null : actor.uid(),
                actor == null ? null : actor.role().name(),
                recipientEmail, recipientName);
    }
}
