package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.appointments.data.CounterRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The one source of appointment numbers: {@code APT-yyyymmdd-####}, per-day sequential
 * and human-readable, so the front desk can read one down a telephone.
 *
 * <h2>Why this is no longer a Singleton</h2>
 *
 * <p>It was {@code AppointmentNumberGenerator.getInstance()} with a
 * {@code ConcurrentHashMap} of {@code AtomicInteger}, and its javadoc argued that being
 * a Singleton "guarantees every part of the application shares the same counter, so two
 * threads can never mint the same number". Within one JVM that was true. Across a
 * restart it was not: the map was empty again, and the first booking after a restart
 * minted a number an existing row already held - and {@code appointment_no} is the
 * primary key.</p>
 *
 * <p>The javadoc also claimed the {@code appointment_counter} table "extends the same
 * guarantee across restarts and multiple nodes". It would have, had anything read it.
 * Nothing did.</p>
 *
 * <p>So uniqueness moved to where the data is. This class now holds no state at all and
 * asks {@link CounterRepository} for each value; the database row lock does the work
 * {@code AtomicInteger} was doing, and it survives a restart. There is still exactly one
 * instance, created by {@code AppContext} - but that is now a fact about wiring rather
 * than a guarantee enforced by a private constructor, which is the honest place for it.
 * A static instance holding a database connection would have been worse than the
 * problem it solved.</p>
 */
public class AppointmentNumberGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CounterRepository counters;

    public AppointmentNumberGenerator(CounterRepository counters) {
        this.counters = counters;
    }

    /**
     * The next number for a date.
     *
     * <p>Must be called inside the booking transaction. If the transaction rolls back
     * the increment rolls back with it, so a failed booking does not burn a number and
     * leave a gap.</p>
     */
    public String next(LocalDate date) {
        String dayKey = date.format(DAY);
        return format(dayKey, counters.nextFor(dayKey));
    }

    /** How many appointments exist for a date, without taking a number. */
    public int issuedFor(LocalDate date) {
        return counters.currentFor(date.format(DAY));
    }

    private static String format(String dayKey, int sequence) {
        return String.format("APT-%s-%04d", dayKey, sequence);
    }
}
