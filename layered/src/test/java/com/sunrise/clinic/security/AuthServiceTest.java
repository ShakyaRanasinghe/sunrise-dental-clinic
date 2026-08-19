package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.repository.inmemory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-S07..S13 — sign-in, including the account lock-out extension from the
 * use case model.
 */
class AuthServiceTest {

    private InMemoryUserRepository users;
    private LoginAttemptService lockService;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        lockService = new LoginAttemptService();
        auth = new AuthService(users, lockService);
        auth.register("nimal@example.lk", "correct-password", "Nimal Perera", Role.PATIENT);
    }

    @Test
    void signsInWithTheRightPassword() {
        AuthService.LoginResult result = auth.login("nimal@example.lk", "correct-password");

        assertTrue(result.success());
        assertNotNull(result.user());
        assertEquals(Role.PATIENT, result.user().getRole());
    }

    @Test
    void ignoresTheCaseAndSpacingOfTheEmail() {
        assertTrue(auth.login("  NIMAL@Example.LK ", "correct-password").success());
    }

    @Test
    void rejectsTheWrongPassword() {
        AuthService.LoginResult result = auth.login("nimal@example.lk", "guess");
        assertFalse(result.success());
        assertNull(result.user());
    }

    @Test
    void doesNotRevealWhetherAnAccountExists() {
        // Identical wording for an unknown address and a wrong password, so the
        // login screen cannot be used to discover who has an account.
        assertEquals(auth.login("nobody@example.lk", "x").message(),
                auth.login("nimal@example.lk", "wrong").message());
    }

    @Test
    void locksTheAccountAfterRepeatedFailures() {
        for (int attempt = 0; attempt < LoginAttemptService.MAX_ATTEMPTS; attempt++) {
            auth.login("nimal@example.lk", "wrong");
        }

        AuthService.LoginResult result = auth.login("nimal@example.lk", "correct-password");
        assertFalse(result.success(), "a locked account must be refused even with the right password");
        assertTrue(result.lockStatus().locked());
        assertTrue(result.lockStatus().retryAfterSeconds() > 0);
    }

    @Test
    void anAdministratorCanUnlockAnAccount() {
        for (int attempt = 0; attempt < LoginAttemptService.MAX_ATTEMPTS; attempt++) {
            auth.login("nimal@example.lk", "wrong");
        }
        auth.unlock("nimal@example.lk");

        assertTrue(auth.login("nimal@example.lk", "correct-password").success());
    }

    @Test
    void aSuccessfulSignInClearsEarlierFailures() {
        auth.login("nimal@example.lk", "wrong");
        auth.login("nimal@example.lk", "wrong");
        auth.login("nimal@example.lk", "correct-password");

        assertEquals(LoginAttemptService.MAX_ATTEMPTS,
                lockService.status("nimal@example.lk").attemptsRemaining());
    }

    @Test
    void refusesToRegisterTheSameEmailTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.register("nimal@example.lk", "another", "Impostor", Role.ADMIN));
    }

    @Test
    void storesOnlyAHashOfThePassword() {
        String stored = users.findByEmail("nimal@example.lk").orElseThrow().getPasswordHash();
        assertFalse(stored.contains("correct-password"));
        assertTrue(PasswordHasher.matches("correct-password", stored));
    }
}
