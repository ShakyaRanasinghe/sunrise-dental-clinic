package com.sunrise.clinic.reporting.service;

import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.access.service.AuthService;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.platform.audit.AuditEvent;
import com.sunrise.clinic.platform.audit.AuditRepository;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Account administration - FR-ADM-20 to FR-ADM-27.
 *
 * <p>Everything here requires {@code MANAGE_ACCOUNTS}, which only the administrator holds,
 * and everything here writes an audit record. An account change with no trail is the one
 * kind of change nobody can answer questions about afterwards.</p>
 */
public class AccountAdminService {

    private static final Logger log = Logger.getLogger(AccountAdminService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Unambiguous characters only: no O/0, no I/l/1, because these get read aloud. */
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final UserAccountFactory factory;
    private final AuthService auth;
    private final DentistRepository dentists;
    private final AuditRepository audit;
    private final com.sunrise.clinic.platform.service.PersonNumberGenerator numbers;

    public AccountAdminService(UserRepository users, UserAccountFactory factory, AuthService auth,
                               DentistRepository dentists, AuditRepository audit) {
        this(users, factory, auth, dentists, audit, null);
    }

    public AccountAdminService(UserRepository users, UserAccountFactory factory, AuthService auth,
                               DentistRepository dentists, AuditRepository audit,
                               com.sunrise.clinic.platform.service.PersonNumberGenerator numbers) {
        this.users = users;
        this.factory = factory;
        this.auth = auth;
        this.dentists = dentists;
        this.audit = audit;
        this.numbers = numbers;
    }

    /** An account as the administration screen lists it. Never carries the hash. */
    public record AccountRow(String uid, String accountNo, String email, String displayName,
                             Role role, boolean active, boolean locked, int failedAttempts) {

        public static AccountRow of(UserAccount account) {
            return new AccountRow(account.getUid(), account.getAccountNo(), account.getEmail(),
                    account.getDisplayName(),
                    account.getRole(), account.isActive(), account.isLocked(),
                    account.getFailedAttempts());
        }

        /** GAP-ADM-11: the quotable number, falling back to the uid where unassigned. */
        public String accountNumber() {
            return accountNo != null ? accountNo : uid;
        }
    }

    /**
     * A newly created account, and the one-time password to hand over - FR-ADM-25.
     *
     * <p>The password exists in this object and nowhere else. It is never stored, never
     * logged, and cannot be read back: the account holds a hash, and this record is
     * discarded when the response is written. If the administrator loses it before the new
     * member of staff has signed in, the account has to be created again or the password
     * reset - which is the correct cost of never being able to look up a password.</p>
     */
    public record NewAccount(AccountRow account, String oneTimePassword) {
    }

    /** Every account, staff first, then patients, alphabetically within each. */
    public List<AccountRow> list(ClinicPrincipal caller) {
        AccessControl.require(caller, Action.MANAGE_ACCOUNTS);
        return users.findAll().stream()
                .sorted(Comparator.comparing((UserAccount a) -> a.getRole() == Role.PATIENT)
                        .thenComparing(UserAccount::getRole)
                        .thenComparing(a -> a.getDisplayName() == null ? "" : a.getDisplayName()))
                .map(AccountRow::of)
                .toList();
    }

    /**
     * Create a staff account - FR-ADM-20.
     *
     * <p>Goes through {@link UserAccountFactory} rather than building the account here, so
     * every account in the system is created one way (FR-ADM-22). The factory refuses
     * {@code PATIENT}, because patients register themselves.</p>
     *
     * <p>Creating a {@code DENTIST} also creates the matching {@code dentist} profile row
     * and links it (FR-ADM-21). Without that, the new dentist can sign in and their
     * schedule answers "no dentist record for u-…" - an account that exists and cannot
     * work.</p>
     */
    public NewAccount createStaff(ClinicPrincipal caller, String email, String displayName,
                                  Role role, String specialization, BigDecimal consultationFee) {
        AccessControl.require(caller, Action.MANAGE_ACCOUNTS);

        String oneTimePassword = generatePassword();
        UserAccount account = factory.createStaff(email, oneTimePassword, displayName, role);

        if (role == Role.DENTIST) {
            Dentist dentist = Dentist.builder()
                    .id(dentistIdFor(displayName))
                    .userUid(account.getUid())
                    .name(displayName)
                    .specialization(specialization == null || specialization.isBlank()
                            ? "General Dentistry" : specialization.trim())
                    .consultationFee(consultationFee == null
                            ? new BigDecimal("1500.00") : consultationFee)
                    .active(true)
                    .build();
            dentists.save(dentist);
            record(caller, "DENTIST_PROFILE_CREATED", "Dentist", dentist.getId());
        }

        record(caller, "ACCOUNT_CREATED", "UserAccount", account.getUid());
        log.log(Level.INFO, "account_created uid={0} role={1} by={2}",
                new Object[] { account.getUid(), role, caller.uid() });
        return new NewAccount(AccountRow.of(account), oneTimePassword);
    }

    /** Clear a lock-out - FR-ADM-23. */
    public AccountRow unlock(ClinicPrincipal caller, String uid) {
        AccessControl.require(caller, Action.MANAGE_ACCOUNTS);
        UserAccount account = require(uid);
        auth.unlock(account.getEmail());
        record(caller, "ACCOUNT_UNLOCKED", "UserAccount", uid);
        return AccountRow.of(require(uid));
    }

    /**
     * Deactivate or reactivate an account - FR-ADM-24.
     *
     * <p>Deactivating rather than deleting: appointments, bills and audit records refer to
     * the account, and a deleted row would take the history with it or leave it dangling.</p>
     *
     * @throws AccessControl.AccessDeniedException if the administrator aims at their own
     *         account - FR-ADM-26. Locking yourself out of the only account that can unlock
     *         accounts leaves the clinic with no way in
     */
    public AccountRow setActive(ClinicPrincipal caller, String uid, boolean active) {
        AccessControl.require(caller, Action.MANAGE_ACCOUNTS);
        if (uid.equals(caller.uid())) {
            throw new AccessControl.AccessDeniedException(
                    "You cannot deactivate your own account. Ask another administrator.");
        }
        UserAccount account = require(uid);
        account.setActive(active);
        users.save(account);
        record(caller, active ? "ACCOUNT_REACTIVATED" : "ACCOUNT_DEACTIVATED", "UserAccount", uid);
        log.log(Level.INFO, "account_active_changed uid={0} active={1} by={2}",
                new Object[] { uid, active, caller.uid() });
        return AccountRow.of(account);
    }

    /**
     * Issue a fresh one-time password - the only way a password is ever changed by an
     * administrator, and they still never learn the old one (FR-ADM-25).
     */
    public NewAccount resetPassword(ClinicPrincipal caller, String uid) {
        AccessControl.require(caller, Action.MANAGE_ACCOUNTS);
        UserAccount account = require(uid);
        String oneTimePassword = generatePassword();
        account.setPasswordHash(
                com.sunrise.clinic.access.service.PasswordHasher.hash(oneTimePassword));
        account.setFailedAttempts(0);
        account.setLocked(false);
        users.save(account);
        record(caller, "PASSWORD_RESET", "UserAccount", uid);
        return new NewAccount(AccountRow.of(account), oneTimePassword);
    }

    private UserAccount require(String uid) {
        return users.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("No account " + uid));
    }

    /** FR-ADM-27: actor, target and time, for every change above. */
    private void record(ClinicPrincipal caller, String action, String targetType, String targetId) {
        audit.save(AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .actorUid(caller.uid())
                .actorRole(caller.role().name())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .timestamp(Instant.now())
                .build());
    }

    private static String generatePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    /**
     * GAP-ADM-11: new dentists join the readable family ({@code YYMMDDDENNNNN});
     * seeded friendly ids stay untouched. Falls back to the old slug only where no
     * generator was wired (older tests).
     */
    private String dentistIdFor(String displayName) {
        if (numbers != null) {
            return numbers.next(java.time.LocalDate.now(), "DEN");
        }
        String base = "d-" + displayName.toLowerCase()
                .replaceAll("^dr\\.?\\s+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (dentists.findById(base).isEmpty()) {
            return base;
        }
        // Two dentists can share a name. The id has to differ, and it is never shown.
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
