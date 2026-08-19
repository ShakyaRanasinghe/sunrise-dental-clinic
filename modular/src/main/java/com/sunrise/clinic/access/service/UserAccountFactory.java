package com.sunrise.clinic.access.service;

import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one place an account comes into existence.
 *
 * <p>Factory Method (FR-ADM-22). Two callers reach it — patient
 * self-registration and the administrator creating staff — and both arrive here
 * rather than each building a {@link UserAccount} themselves. That is what makes
 * the role rule enforceable in one place instead of two.</p>
 *
 * <h2>Why two methods and not one with a role parameter</h2>
 *
 * <p>{@link #registerPatient} takes no role. It cannot create anything but a
 * patient, so a {@code role} field arriving in a form or a JSON body has nothing
 * to bind to — which is FR-PAT-03, and the reason self-registration cannot be
 * used to mint a receptionist. {@link #createStaff} takes an explicit role and
 * refuses {@code PATIENT}, because patients register themselves (ASM-05).</p>
 *
 * <p>A single {@code create(role, …)} would have been shorter and would have put
 * the one rule that matters behind a caller's argument.</p>
 *
 * <p>Profile rows — {@code patient} for a patient, {@code dentist} for a dentist
 * — are created alongside the account from step 3 onward (FR-PAT-04,
 * FR-ADM-21). Until those modules exist this creates the account only, and the
 * gap is recorded rather than hidden.</p>
 */
public class UserAccountFactory {

    private static final Logger log = Logger.getLogger(UserAccountFactory.class.getName());

    private final UserRepository users;

    public UserAccountFactory(UserRepository users) {
        this.users = users;
    }

    /** Creates a patient account. The role is fixed here and nowhere else. */
    public UserAccount registerPatient(String email, String password, String displayName) {
        return create(email, password, displayName, Role.PATIENT);
    }

    /** Creates a staff account. Refuses {@code PATIENT}. */
    public UserAccount createStaff(String email, String password, String displayName, Role role) {
        if (role == Role.PATIENT) {
            throw new IllegalArgumentException(
                    "Patients register themselves; an administrator does not create them.");
        }
        return create(email, password, displayName, role);
    }

    private UserAccount create(String email, String password, String displayName, Role role) {
        String key = normalise(email);
        if (users.findByEmail(key).isPresent()) {
            throw new IllegalArgumentException("An account already exists for " + key);
        }
        UserAccount account = UserAccount.builder()
                .uid(UUID.randomUUID().toString())
                .email(key)
                .passwordHash(PasswordHasher.hash(password))
                .displayName(displayName)
                .role(role)
                .active(true)
                .failedAttempts(0)
                .locked(false)
                .createdAt(Instant.now())
                .build();
        users.save(account);
        log.log(Level.INFO, "account_created email={0} role={1}", new Object[]{key, role});
        return account;
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
