package com.sunrise.clinic.service.notification;

import com.sunrise.clinic.domain.Appointment;

/**
 * A domain event carried by the OBSERVER pattern. Published when an appointment
 * changes state; observers react (send a notification, write an audit record)
 * without the {@code AppointmentService} knowing who is listening.
 *
 * @param type           what happened
 * @param appointment    the affected appointment
 * @param recipientEmail patient email (for the confirmation), may be null
 * @param recipientName  patient display name
 */
public record AppointmentEvent(
        Type type,
        Appointment appointment,
        String recipientEmail,
        String recipientName) {

    public enum Type {
        CREATED,
        CANCELLED,
        COMPLETED,
        BILLED
    }
}
