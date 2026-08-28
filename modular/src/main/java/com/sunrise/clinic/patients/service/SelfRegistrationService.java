package com.sunrise.clinic.patients.service;

import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientResponse;
import com.sunrise.clinic.platform.data.TransactionRunner;

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
    private static final int MIN_PHONE_DIGITS = 7;
    private static final String PHONE_PATTERN = "[0-9+()\\- ]+";

    private final UserAccountFactory accounts;
    private final PatientRepository patients;
    private final TransactionRunner transaction;

    public SelfRegistrationService(UserAccountFactory accounts, PatientRepository patients,
                                   TransactionRunner transaction) {
        this.accounts = accounts;
        this.patients = patients;
        this.transaction = transaction;
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
     * <p>Only four things are actually required: a name, an email, a password and its
     * confirmation. <b>The contact number is optional</b>, and so are the address and the date of
     * birth — the column is nullable and reception can fill any of them in later. Demanding a
     * telephone number at sign-up turns away somebody who would otherwise have become a patient,
     * to collect a field that is more reliably taken at the desk.</p>
     *
     * @throws IllegalArgumentException on anything the person can fix by retyping
     */
    public Registered register(Registration form) {
        String name = required(form.name(), "Your name");
        String email = validEmail(form.email());
        String password = requirePassword(form.password(), form.confirmPassword());
        String contact = contactNumber(form.contactNumber());
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

    private static String contactNumber(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "The contact number can contain digits with + ( ) and - only. No letters.");
        }
        if (trimmed.replaceAll("[^0-9]", "").length() < MIN_PHONE_DIGITS) {
            throw new IllegalArgumentException(
                    "The contact number needs at least " + MIN_PHONE_DIGITS + " digits.");
        }
        return trimmed;
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
