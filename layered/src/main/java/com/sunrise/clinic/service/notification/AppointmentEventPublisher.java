package com.sunrise.clinic.service.notification;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OBSERVER pattern subject. {@code AppContext} registers every
 * {@link AppointmentObserver}; {@link #publish(AppointmentEvent)} fans the event
 * out to all of them.
 * One misbehaving observer never breaks the others (or the triggering action) —
 * each is called defensively.
 */
public class AppointmentEventPublisher {

    private static final Logger log = Logger.getLogger(AppointmentEventPublisher.class.getName());

    private final List<AppointmentObserver> observers;

    public AppointmentEventPublisher(List<AppointmentObserver> observers) {
        this.observers = observers;
    }

    public void publish(AppointmentEvent event) {
        for (AppointmentObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                // Observers are best-effort — log and continue, never propagate.
                log.log(Level.WARNING,
                        "observer_failed observer=" + observer.getClass().getSimpleName(), e);
            }
        }
    }
}
