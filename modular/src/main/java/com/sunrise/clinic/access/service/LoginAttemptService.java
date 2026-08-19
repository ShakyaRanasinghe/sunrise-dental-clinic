package com.sunrise.clinic.access.service;

import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.UserAccount;

import java.util.Optional;

/**
 * Counts failed sign-ins and locks an account after too many.
 *
 * <p><strong>State lives in {@code user_account}, not in memory.</strong> The
 * previous implementation held a {@code ConcurrentHashMap}, which meant every
 * lock was cleared by a restart — so the defence NFR-SEC-03 describes could be
 * defeated by waiting for a deployment. The columns
 * {@code failed_attempts} and {@code locked} already existed and nothing wrote
 * them.</p>
 *
 * <p>A lock is cleared only by an administrator (NFR-SEC-03). There is no
 * timed expiry: the earlier 24-hour auto-unlock was undocumented, and an
 * account that quietly unlocks itself is a weaker guarantee than one that does
 * not.</p>
 */
public class LoginAttemptService {

    /** Consecutive failures that lock an account. */
    public static final int MAX_ATTEMPTS = 5;

    private final UserRepository users;

    public LoginAttemptService(UserRepository users) {
        this.users = users;
    }

    /**
     * The lock state of an account.
     *
     * <p>Carries no retry time, because a lock does not expire — an
     * administrator clears it.</p>
     */
    public record LockStatus(boolean locked, int attemptsRemaining) {

        /** The state of an email that matches no account. */
        static LockStatus unknownAccount() {
            return new LockStatus(false, MAX_ATTEMPTS);
        }
    }

    /** Records one failure and returns the resulting state. */
    public synchronized LockStatus recordFailure(String email) {
        Optional<UserAccount> found = users.findByEmail(email);
        if (found.isEmpty()) {
            // Do not create a row for an address nobody registered - that would
            // turn a failed sign-in into an account-enumeration oracle.
            return LockStatus.unknownAccount();
        }
        UserAccount user = found.get();
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            user.setLocked(true);
        }
        users.save(user);
        return statusOf(user);
    }

    /** Clears the counter after a successful sign-in. */
    public synchronized void recordSuccess(String email) {
        users.findByEmail(email).ifPresent(user -> {
            if (user.getFailedAttempts() != 0 || user.isLocked()) {
                user.setFailedAttempts(0);
                user.setLocked(false);
                users.save(user);
            }
        });
    }

    /** Clears a lock. Administrator only, enforced by the caller. */
    public synchronized void reset(String email) {
        recordSuccess(email);
    }

    /** Whether the account is locked. */
    public boolean isLocked(String email) {
        return users.findByEmail(email).map(UserAccount::isLocked).orElse(false);
    }

    /** The state of the account, or an unlocked default if there is none. */
    public LockStatus status(String email) {
        return users.findByEmail(email).map(this::statusOf).orElseGet(LockStatus::unknownAccount);
    }

    private LockStatus statusOf(UserAccount user) {
        return new LockStatus(user.isLocked(),
                Math.max(0, MAX_ATTEMPTS - user.getFailedAttempts()));
    }
}
