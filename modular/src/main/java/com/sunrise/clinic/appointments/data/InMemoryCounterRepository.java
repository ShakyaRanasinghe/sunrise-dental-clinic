package com.sunrise.clinic.appointments.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link CounterRepository}, for tests.
 *
 * <p>This is what the previous {@code AppointmentNumberGenerator} was in production.
 * Correct for a test, where the process is the whole world; wrong for a deployment,
 * where a restart resets it.</p>
 */
public class InMemoryCounterRepository implements CounterRepository {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public int nextFor(String dayKey) {
        return counters.computeIfAbsent(dayKey, key -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public int currentFor(String dayKey) {
        AtomicInteger counter = counters.get(dayKey);
        return counter == null ? 0 : counter.get();
    }
}
