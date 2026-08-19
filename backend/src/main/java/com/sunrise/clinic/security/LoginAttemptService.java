package com.sunrise.clinic.security;


import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account-lockout state machine (the "lock account after failed attempts" use case).
 * After {@link #MAX_ATTEMPTS} consecutive failed logins for an identity (email/uid), the
 * account is locked for {@link #LOCK_DURATION}. A successful login or an admin reset clears
 * it. Thread-safe and testable with no external services.
 */
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofHours(24);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int count, Instant lockedUntil) {
    }

    /** Result returned to the caller (and, via the controller, to the UI for a countdown). */
    public record LockStatus(boolean locked, int attemptsRemaining, long retryAfterSeconds) {
    }

    public synchronized LockStatus recordFailure(String key) {
        Attempt current = attempts.getOrDefault(key, new Attempt(0, null));
        int count = current.count() + 1;
        Instant lockedUntil = count >= MAX_ATTEMPTS
                ? Instant.now().plus(LOCK_DURATION)
                : current.lockedUntil();
        attempts.put(key, new Attempt(count, lockedUntil));
        return status(key);
    }

    /** Clears the counter on a genuine successful login (only when not currently locked). */
    public synchronized void recordSuccess(String key) {
        if (!isLocked(key)) {
            attempts.remove(key);
        }
    }

    /** Admin-initiated unlock. */
    public synchronized void reset(String key) {
        attempts.remove(key);
    }

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key);
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    public LockStatus status(String key) {
        Attempt a = attempts.getOrDefault(key, new Attempt(0, null));
        boolean locked = isLocked(key);
        int remaining = Math.max(0, MAX_ATTEMPTS - a.count());
        long retryAfter = locked ? Math.max(0, Duration.between(Instant.now(), a.lockedUntil()).getSeconds()) : 0;
        return new LockStatus(locked, remaining, retryAfter);
    }
}
