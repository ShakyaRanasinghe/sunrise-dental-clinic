package com.sunrise.clinic.platform.data;

import java.util.function.Supplier;

/**
 * Runs a unit of work atomically.
 *
 * <p>The service layer needs several repository calls to succeed or fail together —
 * booking an appointment writes a slot and an appointment, and a half-completed
 * booking would corrupt the schedule. With no framework there is no
 * {@code @Transactional} annotation to express that, so it is expressed as an
 * interface the service depends on instead.</p>
 *
 * <p>Two implementations exist: one wrapping a real database transaction, and one
 * that simply serialises the work for the in-memory repositories the unit tests
 * use. Because {@code AppointmentService} depends on this interface rather than on
 * JDBC, the same booking logic is exercised by fast tests and by the running
 * application.</p>
 */
@FunctionalInterface
public interface TransactionRunner {

    /**
     * Execute {@code work} atomically and return its result.
     *
     * <p>If the work throws, nothing it did is visible afterwards and the exception
     * reaches the caller unchanged.</p>
     */
    <T> T execute(Supplier<T> work);

    /** Convenience form for work that returns nothing. */
    default void run(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
