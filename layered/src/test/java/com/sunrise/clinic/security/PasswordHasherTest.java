package com.sunrise.clinic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-S01..S06 — password hashing.
 *
 * <p>The application verifies passwords itself now, so these properties are worth
 * asserting rather than assuming: the stored value never contains the password,
 * the same password hashes differently for different users, and a wrong password
 * is rejected.</p>
 */
class PasswordHasherTest {

    @Test
    void acceptsTheCorrectPassword() {
        String stored = PasswordHasher.hash("correct horse battery");
        assertTrue(PasswordHasher.matches("correct horse battery", stored));
    }

    @Test
    void rejectsAWrongPassword() {
        String stored = PasswordHasher.hash("correct horse battery");
        assertFalse(PasswordHasher.matches("Correct horse battery", stored));
        assertFalse(PasswordHasher.matches("", stored));
        assertFalse(PasswordHasher.matches("correct horse batter", stored));
    }

    @Test
    void neverStoresThePasswordItself() {
        String password = "PlainTextSecret123";
        assertFalse(PasswordHasher.hash(password).contains(password));
    }

    @Test
    void saltsEachHashSeparately() {
        // Two users with the same password must not share a hash, or cracking one
        // would crack both.
        assertNotEquals(PasswordHasher.hash("same password"),
                PasswordHasher.hash("same password"));
    }

    @Test
    void bothHashesOfTheSamePasswordStillVerify() {
        String first = PasswordHasher.hash("same password");
        String second = PasswordHasher.hash("same password");
        assertTrue(PasswordHasher.matches("same password", first));
        assertTrue(PasswordHasher.matches("same password", second));
    }

    @Test
    void refusesValuesItDidNotProduce() {
        // A row corrupted, hand-edited, or left over from another scheme must fail
        // closed rather than being treated as a match.
        assertFalse(PasswordHasher.matches("anything", "not-a-hash"));
        assertFalse(PasswordHasher.matches("anything", ""));
        assertFalse(PasswordHasher.matches("anything", null));
        assertFalse(PasswordHasher.matches(null, PasswordHasher.hash("x")));
    }

    @Test
    void recordsTheIterationCountSoItCanBeRaisedLater() {
        String stored = PasswordHasher.hash("x");
        String[] parts = stored.split(":");
        assertTrue(parts.length == 3, "expected iterations:salt:hash");
        assertTrue(Integer.parseInt(parts[0]) >= 100_000,
                "the work factor must stay high enough to slow brute force");
    }
}
