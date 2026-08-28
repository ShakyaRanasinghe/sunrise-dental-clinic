package com.sunrise.clinic.access;

import com.sunrise.clinic.access.data.InMemoryUserRepository;
import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.LoginAttemptService;
import com.sunrise.clinic.access.service.LoginAttemptService.LockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} in step 2 and rewritten, because the service it
 * tests changed shape.
 *
 * <p>In {@code layered/} the counters lived in a {@code ConcurrentHashMap}, so a
 * restart cleared every lock — five failures, restart Tomcat, five more, with no
 * limit that meant anything. Here they live in {@code user_account}, so the
 * service takes a {@link UserRepository} and the test drives it through one.</p>
 *
 * <p>There is deliberately no test for a lock expiring. It does not expire: the
 * earlier 24-hour auto-unlock was dropped, and only an administrator clears a
 * lock (NFR-SEC-03).</p>
 */
class LoginAttemptServiceTest {

    private static final String EMAIL = "nimal@example.lk";

    private UserRepository users;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        users.save(new UserAccount("u-1", EMAIL, "1000:x:y", "Nimal",
                Role.PATIENT, true, 0, false, Instant.now()));
        service = new LoginAttemptService(users);
    }

    @Test
    void locksAfterFiveConsecutiveFailures() {
        for (int i = 1; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertFalse(service.recordFailure(EMAIL).locked(), "must not lock on failure " + i);
        }
        assertTrue(service.recordFailure(EMAIL).locked(), "the fifth failure locks");
        assertTrue(service.isLocked(EMAIL));
    }

    @Test
    void countsDownTheRemainingAttempts() {
        assertEquals(5, service.status(EMAIL).attemptsRemaining());
        assertEquals(4, service.recordFailure(EMAIL).attemptsRemaining());
        assertEquals(3, service.recordFailure(EMAIL).attemptsRemaining());
    }

    @Test
    void anAdministratorResetUnlocks() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(EMAIL);
        }
        assertTrue(service.isLocked(EMAIL));

        service.reset(EMAIL);

        assertFalse(service.isLocked(EMAIL));
        assertEquals(5, service.status(EMAIL).attemptsRemaining());
    }

    @Test
    void successClearsEarlierFailures() {
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);

        service.recordSuccess(EMAIL);

        assertEquals(5, service.status(EMAIL).attemptsRemaining());
        assertFalse(service.isLocked(EMAIL));
    }

    @Test
    void theCountSurvivesANewServiceInstance() {
        // The reason the counters moved into the database. A restart used to
        // clear them, which made the limit meaningless.
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);

        LoginAttemptService restarted = new LoginAttemptService(users);

        assertEquals(3, restarted.status(EMAIL).attemptsRemaining());
    }

    @Test
    void doesNotCreateARowForAnUnknownAddress() {
        // Recording a failure against an address with no account would let a
        // caller discover which addresses are registered by watching the table
        // grow, and would let anyone fill it with rows.
        long before = users.count();

        LockStatus status = service.recordFailure("stranger@example.lk");

        assertEquals(before, users.count());
        assertFalse(status.locked());
        assertEquals(5, status.attemptsRemaining(), "must look like an untouched account");
    }
}
