package com.sunrise.clinic.access.service;

import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authenticates users and registers new accounts.
 *
 * <p>This is the "Authenticate Credentials" use case that the login flow includes,
 * and the place where the "Lock Account After Failed Attempts" extension is
 * enforced. The order of the checks matters and is deliberate:</p>
 *
 * <ol>
 *   <li>refuse immediately if the identity is locked out;</li>
 *   <li>look the account up and verify the password;</li>
 *   <li>on failure record the attempt, which may trigger the lock;</li>
 *   <li>on success clear the counter and return the account.</li>
 * </ol>
 *
 * <p>A failed login returns the same generic outcome whether the email is unknown
 * or the password is wrong. Distinguishing them would let an attacker enumerate
 * which addresses have accounts.</p>
 */
public class AuthService {
    /*
     * Authenticates and unlocks. It does not create accounts - UserAccountFactory
     * does, so that the role rule lives in one place (FR-ADM-22). Moving it out
     * also removed this class's only reason to know about Role.
     */

    private static final Logger log = Logger.getLogger(AuthService.class.getName());

    /** The outcome of a login attempt. */
    public record LoginResult(boolean success,
                              UserAccount user,
                              String message,
                              LoginAttemptService.LockStatus lockStatus) {

        public static LoginResult ok(UserAccount user, LoginAttemptService.LockStatus status) {
            return new LoginResult(true, user, "Signed in", status);
        }

        public static LoginResult failed(String message, LoginAttemptService.LockStatus status) {
            return new LoginResult(false, null, message, status);
        }
    }

    private final UserRepository users;
    private final LoginAttemptService lockService;

    public AuthService(UserRepository users, LoginAttemptService lockService) {
        this.users = users;
        this.lockService = lockService;
    }

    /**
     * Attempt to sign a user in.
     *
     * <p>GAP-ADM-12: the identifier is a staff username or an email address.
     * Usernames cannot contain {@code @}, so anything holding one is looked up by
     * username first; an address goes straight to the email lookup. Either way the
     * failure message matches what the screen asked for, and stays identical for
     * "no such user" and "wrong password".
     *
     * @param identifier the username or address typed at the login screen
     * @param password   the password typed at the login screen
     * @return the result, never null
     */
    public LoginResult login(String identifier, String password) {
        String key = normalise(identifier);
        boolean byUsername = !key.contains("@");
        String wrongMessage = byUsername ? "Incorrect username or password."
                : "Incorrect email or password.";

        if (lockService.isLocked(key)) {
            log.log(Level.INFO, "login_blocked_locked identity={0}", key);
            return LoginResult.failed(
                    "This account is locked. Please contact the clinic to have it unlocked.",
                    lockService.status(key));
        }

        Optional<UserAccount> found = byUsername ? users.findByUsername(key) : Optional.empty();
        if (found.isEmpty()) {
            found = users.findByEmail(key);
        }
        boolean valid = found
                .filter(UserAccount::isActive)
                .map(user -> PasswordHasher.matches(password, user.getPasswordHash()))
                .orElse(false);

        if (!valid) {
            LoginAttemptService.LockStatus status = lockService.recordFailure(key);
            log.log(Level.INFO, "login_failed identity={0} attemptsRemaining={1}",
                    new Object[]{key, status.attemptsRemaining()});
            // Deliberately identical message for "no such user" and "wrong password".
            return LoginResult.failed(wrongMessage, status);
        }

        lockService.recordSuccess(key);
        log.log(Level.INFO, "login_success identity={0}", key);
        return LoginResult.ok(found.get(), lockService.status(key));
    }

    /**
     * Create an account. Used by patient self-registration and by an
     * administrator adding a member of staff.
     *
     * @throws IllegalArgumentException if the email is already registered
     */
    public void unlock(String email) {
        lockService.reset(normalise(email));
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
