package com.sunrise.clinic.security;

import com.sunrise.clinic.security.LoginAttemptService.LockStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-CLI-SEC-01..04 — account-lockout state machine. */
class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void locksAfterFiveConsecutiveFailures() {
        for (int i = 0; i < 4; i++) {
            assertFalse(service.recordFailure("a@x.lk").locked(), "still unlocked at attempt " + (i + 1));
        }
        LockStatus fifth = service.recordFailure("a@x.lk");
        assertTrue(fifth.locked(), "locked on the 5th failure");
        assertTrue(service.isLocked("a@x.lk"));
        assertTrue(fifth.retryAfterSeconds() > 0);
    }

    @Test
    void reportsRemainingAttempts() {
        assertEquals(4, service.recordFailure("b@x.lk").attemptsRemaining());
        assertEquals(3, service.recordFailure("b@x.lk").attemptsRemaining());
    }

    @Test
    void adminResetUnlocks() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("c@x.lk");
        }
        assertTrue(service.isLocked("c@x.lk"));
        service.reset("c@x.lk");
        assertFalse(service.isLocked("c@x.lk"));
    }

    @Test
    void successClearsTheCounter() {
        service.recordFailure("d@x.lk");
        service.recordFailure("d@x.lk");
        service.recordSuccess("d@x.lk");
        assertEquals(LoginAttemptService.MAX_ATTEMPTS, service.status("d@x.lk").attemptsRemaining());
    }
}
