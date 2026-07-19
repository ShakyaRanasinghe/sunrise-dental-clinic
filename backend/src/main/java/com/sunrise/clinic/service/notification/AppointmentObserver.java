package com.sunrise.clinic.service.notification;

/**
 * OBSERVER pattern — an interested party that reacts to {@link AppointmentEvent}s.
 * Implementations are auto-discovered by Spring and registered with the
 * {@link AppointmentEventPublisher}.
 */
public interface AppointmentObserver {
    void onEvent(AppointmentEvent event);
}
