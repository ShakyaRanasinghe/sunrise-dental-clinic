package com.sunrise.clinic.access;

import com.sunrise.clinic.access.data.InMemoryUserRepository;
import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.AuthService;
import com.sunrise.clinic.access.service.AuthService.LoginResult;
import com.sunrise.clinic.access.service.LoginAttemptService;
import com.sunrise.clinic.access.service.UserAccountFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Moved from {@code layered/} in step 2. No database, no container. */
class AuthServiceTest {

    private static final String EMAIL = "nimal@example.lk";
    private static final String PASSWORD = "Password123";

    private UserRepository users;
    private UserAccountFactory factory;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        factory = new UserAccountFactory(users);
        auth = new AuthService(users, new LoginAttemptService(users));
        factory.registerPatient(EMAIL, PASSWORD, "Nimal Perera");
    }

    @Test
    void signsInWithTheRightPassword() {
        LoginResult result = auth.login(EMAIL, PASSWORD);

        assertTrue(result.success());
        assertNotNull(result.user());
        assertEquals(Role.PATIENT, result.user().getRole());
    }

    @Test
    void ignoresTheCaseAndSpacingOfTheEmail() {
        assertTrue(auth.login("  NIMAL@Example.LK  ", PASSWORD).success());
    }

    @Test
    void rejectsTheWrongPassword() {
        LoginResult result = auth.login(EMAIL, "Password124");

        assertFalse(result.success());
        assertNull(result.user());
    }

    @Test
    void doesNotRevealWhetherAnAccountExists() {
        // FR-AUTH-03. A different message for an unknown address would let anyone
        // test whether a person is a patient here, which is itself medical
        // information.
        LoginResult unknown = auth.login("stranger@example.lk", PASSWORD);
        LoginResult wrongPassword = auth.login(EMAIL, "Password124");

        assertFalse(unknown.success());
        assertFalse(wrongPassword.success());
        assertEquals(wrongPassword.message(), unknown.message());
    }

    @Test
    void locksTheAccountAfterRepeatedFailures() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            auth.login(EMAIL, "Password124");
        }

        LoginResult afterLock = auth.login(EMAIL, PASSWORD);

        assertFalse(afterLock.success(), "the correct password must not open a locked account");
        assertTrue(afterLock.lockStatus().locked());
    }

    @Test
    void anAdministratorCanUnlockAnAccount() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            auth.login(EMAIL, "Password124");
        }
        assertTrue(auth.login(EMAIL, PASSWORD).lockStatus().locked());

        auth.unlock(EMAIL);

        assertTrue(auth.login(EMAIL, PASSWORD).success());
    }

    @Test
    void aSuccessfulSignInClearsEarlierFailures() {
        auth.login(EMAIL, "Password124");
        auth.login(EMAIL, "Password124");

        assertTrue(auth.login(EMAIL, PASSWORD).success());

        // Four more failures must not lock, because the counter restarted.
        for (int i = 0; i < 4; i++) {
            assertFalse(auth.login(EMAIL, "Password124").lockStatus().locked());
        }
    }

    @Test
    void refusesToRegisterTheSameEmailTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.registerPatient(EMAIL, PASSWORD, "Someone Else"));
    }

    @Test
    void anAdministratorCannotCreateAPatient() {
        // Patients register themselves; the factory has no route for staff to
        // create one, so a submitted role cannot become PATIENT by mistake.
        assertThrows(IllegalArgumentException.class,
                () -> factory.createStaff("new@example.lk", PASSWORD, "X", Role.PATIENT));
    }

    @Test
    void storesOnlyAHashOfThePassword() {
        UserAccount stored = users.findByEmail(EMAIL).orElseThrow();

        assertFalse(stored.getPasswordHash().contains(PASSWORD));
        assertTrue(stored.getPasswordHash().matches("^\\d+:.+"));
    }

    @Test
    void refusesAnInactiveAccount() {
        UserAccount account = users.findByEmail(EMAIL).orElseThrow();
        account.setActive(false);
        users.save(account);

        assertFalse(auth.login(EMAIL, PASSWORD).success());
    }
}
