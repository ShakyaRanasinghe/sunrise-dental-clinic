package com.sunrise.clinic.platform.data;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * {@link TransactionRunner} for the in-memory repositories used by the unit tests.
 *
 * <p>There is no database to commit to, so the guarantee it provides is the one
 * that actually matters to the booking logic: <b>mutual exclusion</b>. Only one
 * unit of work runs at a time, so a check-then-act sequence such as "is this slot
 * OPEN? then book it" cannot interleave with another thread's. That is what lets
 * the concurrency test for double-booking run against plain maps.</p>
 *
 * <p>What it does <em>not</em> provide is rollback: if the work throws halfway
 * through, earlier writes stay in the maps. That is acceptable for tests, which
 * start from a clean store each time, and is precisely why production uses the
 * JDBC implementation instead.</p>
 *
 * <p>The lock is reentrant so nested units of work behave like the JDBC runner,
 * which joins an outer transaction rather than deadlocking against it.</p>
 */
public class SerialTransactionRunner implements TransactionRunner {

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public <T> T execute(Supplier<T> work) {
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }
}
