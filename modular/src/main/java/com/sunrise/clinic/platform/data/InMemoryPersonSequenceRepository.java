package com.sunrise.clinic.platform.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory {@link PersonSequenceRepository}, so numbering tests need no database. */
public class InMemoryPersonSequenceRepository implements PersonSequenceRepository {

    private final Map<String, AtomicInteger> sequences = new ConcurrentHashMap<>();

    @Override
    public int nextFor(String key) {
        return sequences.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }
}
