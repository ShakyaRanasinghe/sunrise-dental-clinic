package com.sunrise.clinic.pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-A01..A04 — Singleton appointment-number generator.
 */
class AppointmentNumberGeneratorTest {

    private final AppointmentNumberGenerator gen = AppointmentNumberGenerator.getInstance();

    @BeforeEach
    void reset() {
        gen.reset();
    }

    @Test
    void isASingleton() {
        assertSame(AppointmentNumberGenerator.getInstance(), AppointmentNumberGenerator.getInstance());
    }

    @Test
    void generatesSequentialNumbersPerDay() {
        LocalDate day = LocalDate.of(2026, 7, 20);
        assertEquals("APT-20260720-0001", gen.next(day));
        assertEquals("APT-20260720-0002", gen.next(day));
        assertEquals("APT-20260720-0003", gen.next(day));
    }

    @Test
    void countersAreIndependentPerDay() {
        assertEquals("APT-20260720-0001", gen.next(LocalDate.of(2026, 7, 20)));
        assertEquals("APT-20260721-0001", gen.next(LocalDate.of(2026, 7, 21)));
    }

    @Test
    void producesUniqueNumbersUnderConcurrency() throws InterruptedException {
        LocalDate day = LocalDate.of(2026, 7, 20);
        int n = 500;
        Set<String> results = Collections.synchronizedSet(new HashSet<>());
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> results.add(gen.next(day)));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(n, results.size(), "every appointment number must be unique");
    }
}
