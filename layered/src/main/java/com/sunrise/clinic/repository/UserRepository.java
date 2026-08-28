package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.UserAccount;

import java.util.Optional;

/**
 * Persistence for {@link UserAccount}s.
 *
 * <p>New in the framework-free build. Identity used to be held by an external
 * authentication service, so there was nothing to store; now that the application
 * verifies passwords itself, the account — including its hash and lock-out
 * counters — lives in our own database.</p>
 */
public interface UserRepository extends Repository<UserAccount, String> {

    /** Look up an account by the address the user types at the login screen. */
    Optional<UserAccount> findByEmail(String email);
}
