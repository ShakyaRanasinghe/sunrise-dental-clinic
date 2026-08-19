package com.sunrise.clinic.access;

import com.sunrise.clinic.access.service.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} in step 2. Needs no database and no container:
 * the hasher is pure computation, which is the point of keeping it in the
 * service tier with no dependency on the data tier.
 */
class PasswordHasherTest {

    private static final String PASSWORD = "Password123";

    @Test
    void acceptsTheCorrectPassword() {
        assertTrue(PasswordHasher.matches(PASSWORD, PasswordHasher.hash(PASSWORD)));
    }

    @Test
    void rejectsAWrongPassword() {
        String stored = PasswordHasher.hash(PASSWORD);
        assertFalse(PasswordHasher.matches("Password124", stored));
        assertFalse(PasswordHasher.matches("", stored));
        assertFalse(PasswordHasher.matches("password123", stored), "must be case sensitive");
    }

    @Test
    void neverStoresThePasswordItself() {
        assertFalse(PasswordHasher.hash(PASSWORD).contains(PASSWORD));
    }

    @Test
    void saltsEachHashSeparately() {
        // Two hashes of one password must differ, or identical passwords would be
        // visible as identical rows and one cracked hash would open every account.
        assertNotEquals(PasswordHasher.hash(PASSWORD), PasswordHasher.hash(PASSWORD));
    }

    @Test
    void bothHashesOfTheSamePasswordStillVerify() {
        assertTrue(PasswordHasher.matches(PASSWORD, PasswordHasher.hash(PASSWORD)));
        assertTrue(PasswordHasher.matches(PASSWORD, PasswordHasher.hash(PASSWORD)));
    }

    @Test
    void refusesValuesItDidNotProduce() {
        // A malformed or empty column must fail closed rather than throw, so a
        // corrupt row cannot become a way in.
        assertFalse(PasswordHasher.matches(PASSWORD, ""));
        assertFalse(PasswordHasher.matches(PASSWORD, "not-a-hash"));
        assertFalse(PasswordHasher.matches(PASSWORD, "1000:short"));
    }

    @Test
    void recordsTheIterationCountSoItCanBeRaisedLater() {
        // The cost is stored with the hash, so raising it does not invalidate
        // every existing password.
        assertTrue(PasswordHasher.hash(PASSWORD).matches("^\\d+:.+"));
    }
}
