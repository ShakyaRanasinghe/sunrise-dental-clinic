package com.sunrise.clinic.patients.service;

import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientResponse;
import com.sunrise.clinic.platform.data.TransactionRunner;
import com.sunrise.clinic.platform.service.PhoneNumbers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Patient self-registration - FR-PAT-04, FR-PAT-07.
 *
 * <p>Creates <b>two</b> linked records: the {@link UserAccount} that lets somebody sign in, and
 * the {@link Patient} profile that appointments, bills and clinical records attach to. The class
 * diagram keeps them apart on purpose — "who can sign in" is not the same thing as "who they are
 * in the clinic", and a walk-in has the second without the first. From the person filling in the
 * form it is one action, so this makes both.</p>
 *
 * <p><b>In one transaction</b>, because either half alone is worse than neither. An account with
 * no profile signs in successfully and then fails at the first thing it tries to do; a profile
 * with no account is a patient record nobody can reach.</p>
 *
 * <h2>Why this is its own class</h2>
 *
 * <p>It is the only public write endpoint in the system (FR-PAT-07) — the one operation anybody
 * can invoke without signing in first. Every other service method here takes a
 * {@code ClinicPrincipal} and asks {@code AccessControl} about it. This one has no caller to ask
 * about, and that difference is worth being visible in the type rather than buried as a method
 * on {@link PatientService} that quietly skips the check its neighbours all perform.</p>
 *
 * <p>It lives in {@code patients} rather than {@code access} because it writes a patient row, and
 * {@code patients} already depends on {@code access} — the other direction would be a cycle.</p>
 */
public class SelfRegistrationService {

    private static final Logger log = Logger.getLogger(SelfRegistrationService.class.getName());

    private static final int MIN_PASSWORD = 8;
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private final UserAccountFactory accounts;
    private final PatientRepository patients;
    private final TransactionRunner transaction;
    private final com.sunrise.clinic.platform.service.PersonNumberGenerator numbers;

    public SelfRegistrationService(UserAccountFactory accounts, PatientRepository patients,
                                   TransactionRunner transaction) {
        this(accounts, patients, transaction, null);
    }

    public SelfRegistrationService(UserAccountFactory accounts, PatientRepository patients,
                                   TransactionRunner transaction,
                                   com.sunrise.clinic.platform.service.PersonNumberGenerator numbers) {
        this.accounts = accounts;
        this.patients = patients;
        this.transaction = transaction;
        this.numbers = numbers;
    }

    /** What somebody types on the registration form. */
    public record Registration(String name,
                               String email,
                               String password,
                               String confirmPassword,
                               String contactNumber,
                               String address,
                               String dob) {
    }

    /** The account to sign in with, and the patient record behind it. */
    public record Registered(UserAccount account, PatientResponse patient) {
    }

    /**
     * Register somebody.
     *
     * <p>Five things are required: a name, an email, a password and its confirmation, and a
     * contact number (GAP-PAT-20). The address and the date of birth are optional — the columns
     * are nullable and reception can fill either in later. The contact number is demanded because
     * it is the clinic's one reliable way to reach the patient and the key that ties an
     * account to its record.</p>
     *
     * @throws IllegalArgumentException on anything the person can fix by retyping
     */
    public Registered register(Registration form) {
        String name = required(form.name(), "Your name");
        String email = validEmail(form.email());
        String password = requirePassword(form.password(), form.confirmPassword());
        String contact = contactNumber(form.contactNumber(), name);
        String address = trimToNull(form.address());
        LocalDate dob = parseDob(form.dob());

        return transaction.execute(() -> {
            // The factory refuses a duplicate email, so there is no pre-check: asking and then
            // acting is a race, and it would duplicate the rule.
            UserAccount account = accounts.registerPatient(email, password, name);

            Patient patient = Patient.builder()
                    .id(UUID.randomUUID().toString())
                    // Linked, which is what makes this account able to book. The walk-in path
                    // deliberately leaves this null; self-registration is the one place it is set.
                    .userUid(account.getUid())
                    // GAP-PAT-33: inside the transaction, so a rollback reclaims the number.
                    .patientNo(numbers == null ? null
                            : numbers.next(java.time.LocalDate.now(), "PAT"))
                    .name(name)
                    .email(email)
                    .contactNumber(contact)
                    .address(address)
                    .dob(dob)
                    .build();
            patients.save(patient);

            log.log(Level.INFO, "patient_self_registered uid={0} patient={1}",
                    new Object[] { account.getUid(), patient.getId() });
            return new Registered(account, PatientResponse.of(patient));
        });
    }

    private static String requirePassword(String password, String confirmation) {
        String chosen = password == null ? "" : password;
        if (chosen.isBlank()) {
            throw new IllegalArgumentException("Please choose a password.");
        }
        if (!chosen.equals(confirmation)) {
            throw new IllegalArgumentException("The two passwords do not match.");
        }
        if (chosen.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException(
                    "Please choose a password of at least " + MIN_PASSWORD + " characters.");
        }
        return chosen;
    }

    private static String validEmail(String email) {
        String trimmed = required(email, "Your email address").toLowerCase();
        if (!trimmed.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("That does not look like an email address.");
        }
        return trimmed;
    }

    private static String contactNumber(String value, String name) {
        // Required since GAP-PAT-20: a number is how the clinic reaches the patient and is the
        // only stable key across records. Form carries `required`; here is the final gate.
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Your contact number is required.");
        }
        return PhoneNumbers.validate(value, "The contact number");
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDate parseDob(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please give your date of birth as yyyy-MM-dd.");
        }
        if (parsed.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("That date of birth is in the future.");
        }
        return parsed;
    }
}
