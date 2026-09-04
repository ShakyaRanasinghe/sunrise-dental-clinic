package com.sunrise.clinic.access.data;

import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.platform.data.Repository;

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

    /** Look up a staff account by its sign-in name (GAP-ADM-12). */
    Optional<UserAccount> findByUsername(String username);
}
