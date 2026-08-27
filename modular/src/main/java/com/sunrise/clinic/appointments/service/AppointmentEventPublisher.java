package com.sunrise.clinic.appointments.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OBSERVER pattern subject. {@code AppContext} registers every
 * {@link AppointmentObserver}; {@link #publish(AppointmentEvent)} fans the event
 * out to all of them.
 * <p>One misbehaving observer never breaks the others, and never breaks the action that
 * triggered it. That matters most for booking: the appointment is already committed by
 * the time this runs, so throwing here would report a failure for a booking that
 * happened - the patient would be told to try again and would end up with two.</p>
 */
public class AppointmentEventPublisher {

    private static final Logger log = Logger.getLogger(AppointmentEventPublisher.class.getName());

    private final List<AppointmentObserver> observers;

    /**
     * @param observers everything listening. Copied, so the set of observers is fixed
     *                  once the application has started - an observer appearing later
     *                  would mean some appointments were audited and others were not
     */
    public AppointmentEventPublisher(List<AppointmentObserver> observers) {
        this.observers = List.copyOf(observers);
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
