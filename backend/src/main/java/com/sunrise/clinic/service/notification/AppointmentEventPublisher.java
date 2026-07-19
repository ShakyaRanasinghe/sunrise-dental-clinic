package com.sunrise.clinic.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OBSERVER pattern subject. Spring injects every {@link AppointmentObserver}
 * bean; {@link #publish(AppointmentEvent)} fans the event out to all of them.
 * One misbehaving observer never breaks the others (or the triggering action) —
 * each is called defensively.
 */
@Component
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);

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
                log.warn("observer_failed observer={} error={}",
                        observer.getClass().getSimpleName(), e.toString());
            }
        }
    }
}
