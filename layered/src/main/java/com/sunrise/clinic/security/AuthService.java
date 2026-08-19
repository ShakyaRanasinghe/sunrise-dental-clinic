package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.UserAccount;
import com.sunrise.clinic.repository.UserRepository;

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
     * @param email    the address typed at the login screen
     * @param password the password typed at the login screen
     * @return the result, never null
     */
    public LoginResult login(String email, String password) {
        String key = normalise(email);

        if (lockService.isLocked(key)) {
            log.log(Level.INFO, "login_blocked_locked email={0}", key);
            return LoginResult.failed(
                    "This account is temporarily locked. Please try again later.",
                    lockService.status(key));
        }

        Optional<UserAccount> found = users.findByEmail(key);
        boolean valid = found
                .filter(UserAccount::isActive)
                .map(user -> PasswordHasher.matches(password, user.getPasswordHash()))
                .orElse(false);

        if (!valid) {
            LoginAttemptService.LockStatus status = lockService.recordFailure(key);
            log.log(Level.INFO, "login_failed email={0} attemptsRemaining={1}",
                    new Object[]{key, status.attemptsRemaining()});
            // Deliberately identical message for "no such user" and "wrong password".
            return LoginResult.failed("Incorrect email or password.", status);
        }

        lockService.recordSuccess(key);
        log.log(Level.INFO, "login_success email={0}", key);
        return LoginResult.ok(found.get(), lockService.status(key));
    }

    /**
     * Create an account. Used by patient self-registration and by an
     * administrator adding a member of staff.
     *
     * @throws IllegalArgumentException if the email is already registered
     */
    public UserAccount register(String email, String password, String displayName, Role role) {
        String key = normalise(email);
        if (users.findByEmail(key).isPresent()) {
            throw new IllegalArgumentException("An account already exists for " + key);
        }
        UserAccount user = UserAccount.builder()
                .uid(UUID.randomUUID().toString())
                .email(key)
                .passwordHash(PasswordHasher.hash(password))
                .displayName(displayName)
                .role(role)
                .active(true)
                .createdAt(Instant.now())
                .build();
        users.save(user);
        log.log(Level.INFO, "account_registered email={0} role={1}", new Object[]{key, role});
        return user;
    }

    /** Administrator-initiated unlock. */
    public void unlock(String email) {
        lockService.reset(normalise(email));
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
