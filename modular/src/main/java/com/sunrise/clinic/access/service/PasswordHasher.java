package com.sunrise.clinic.access.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password hashing and verification.
 *
 * <p>Credentials used to be held by an external identity service. Now that the
 * application authenticates users itself it must store passwords safely, and the
 * rules it follows are the standard ones:</p>
 *
 * <ul>
 *   <li><b>Never store the password.</b> Only a one-way hash is written to the
 *       database, so a leaked table does not reveal anyone's password.</li>
 *   <li><b>Salt every hash.</b> A fresh random salt per user means two people who
 *       choose the same password get different hashes, which defeats rainbow
 *       tables.</li>
 *   <li><b>Make it deliberately slow.</b> PBKDF2 with {@value #ITERATIONS}
 *       iterations costs a few milliseconds for one login but makes brute-forcing
 *       the whole table impractical. A plain SHA-256 would be far too fast.</li>
 *   <li><b>Compare in constant time.</b> {@link MessageDigest#isEqual} does not
 *       return early on the first differing byte, so the comparison cannot leak
 *       information through its timing.</li>
 * </ul>
 *
 * <p>PBKDF2 is part of the Java platform ({@code javax.crypto}), so this needs no
 * third-party library.</p>
 *
 * <p>The stored format is {@code iterations:salt:hash}, with salt and hash
 * Base64-encoded. Keeping the iteration count inside the string means it can be
 * raised later without invalidating existing passwords.</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordHasher() {
    }

    /**
     * Hash a new password.
     *
     * @param plainPassword the password as typed by the user
     * @return the string to store in {@code user_account.password_hash}
     */
    public static String hash(char[] plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(plainPassword, salt, ITERATIONS);
        return ITERATIONS + ":" + ENCODER.encodeToString(salt) + ":" + ENCODER.encodeToString(hash);
    }

    /** Convenience overload for callers that already hold a String. */
    public static String hash(String plainPassword) {
        return hash(plainPassword.toCharArray());
    }

    /**
     * Check a password against a stored hash.
     *
     * @param plainPassword the password as typed at the login screen
     * @param stored        the value from {@code user_account.password_hash}
     * @return {@code true} only if the password matches
     */
    public static boolean matches(String plainPassword, String stored) {
        if (plainPassword == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            // Not a hash this class produced — refuse rather than guess.
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = DECODER.decode(parts[1]);
            byte[] expected = DECODER.decode(parts[2]);
            byte[] actual = derive(plainPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 is unavailable in this JVM", e);
        }
    }
}
