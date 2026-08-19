package com.sunrise.clinic.pattern;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SINGLETON pattern — one, and only one, source of appointment numbers.
 *
 * <p>Produces a unique, human-readable, per-day sequential number of the form
 * {@code APT-YYYYMMDD-####} (e.g. {@code APT-20260720-0001}). Making this a
 * Singleton guarantees every part of the application shares the same counter,
 * so two threads can never mint the same number.</p>
 *
 * <p><b>Thread safety:</b> a {@link ConcurrentHashMap} of {@link AtomicInteger}
 * counters (one per day) makes {@link #next(LocalDate)} atomic without locking,
 * which is sufficient within a single JVM. The {@code appointment_counter} table
 * in the schema extends the same guarantee across restarts and multiple nodes.</p>
 */
public final class AppointmentNumberGenerator {

    /** The single eagerly-created instance. */
    private static final AppointmentNumberGenerator INSTANCE = new AppointmentNumberGenerator();

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** One monotonic counter per calendar day. */
    private final Map<String, AtomicInteger> dailyCounters = new ConcurrentHashMap<>();

    /** Private constructor — no other class may instantiate this. */
    private AppointmentNumberGenerator() {
    }

    /** @return the one shared instance. */
    public static AppointmentNumberGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * Atomically produce the next appointment number for the given date.
     *
     * @param date the appointment date (defines the daily bucket)
     * @return e.g. {@code APT-20260720-0001}
     */
    public String next(LocalDate date) {
        String dayKey = date.format(DAY);
        int seq = dailyCounters
                .computeIfAbsent(dayKey, k -> new AtomicInteger(0))
                .incrementAndGet();
        return String.format("APT-%s-%04d", dayKey, seq);
    }

    /** Test-only hook to reset counters between test cases. */
    public void reset() {
        dailyCounters.clear();
    }
}
