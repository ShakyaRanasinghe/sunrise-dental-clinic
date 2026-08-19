package com.sunrise.clinic.service.notification;

/**
 * OBSERVER pattern — an interested party that reacts to {@link AppointmentEvent}s.
 * Implementations are registered with the {@link AppointmentEventPublisher}
 * by {@code AppContext} at start-up.
 */
public interface AppointmentObserver {
    void onEvent(AppointmentEvent event);
}
