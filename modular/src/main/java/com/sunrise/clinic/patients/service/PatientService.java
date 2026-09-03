package com.sunrise.clinic.patients.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientResponse;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.service.PhoneNumbers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The patient register.
 *
 * <p>New in the modular build. In {@code layered/} there was no patient service at
 * all: {@code PatientApiServlet} and {@code PatientRecordsServlet} each reached the
 * repository directly and each carried its own copy of the email pattern, the
 * required-field checks and the identifier generation - which is how they came to
 * disagree. The JSON route linked a new record to whoever was signed in; the HTML
 * route did not. One of those was a defect, and having two copies is what let it
 * hide.</p>
 *
 * <p>Every method takes the caller and asks {@link AccessControl} about an
 * {@link Action}. That keeps the rule beside the operation, and means a second
 * caller - a new servlet, a scheduled job - cannot reach the register without
 * passing the same check.</p>
 */
public class PatientService {

    private static final Logger log = Logger.getLogger(PatientService.class.getName());

    /**
     * Permissive on purpose: enough to catch a typed mistake, not so strict that it
     * rejects a legitimate address. A receptionist retyping a patient's email at a
     * busy desk is better served by a rule that only rejects the obvious.
     */
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private final PatientRepository patients;

    public PatientService(PatientRepository patients) {
        this.patients = patients;
    }

    /**
     * The register, or the subset matching {@code term}.
     *
     * <p>A blank or absent term lists everyone, which is what the front desk wants
     * when it opens the screen.</p>
     */
    public List<PatientResponse> search(ClinicPrincipal caller, String term) {
        AccessControl.require(caller, Action.SEARCH_PATIENTS);
        List<Patient> found = (term == null || term.isBlank())
                ? patients.findAll()
                : patients.search(term.trim());
        return found.stream().map(PatientResponse::of).toList();
    }

    /**
     * One page of the register, with the totals the screen needs to draw the
     * pager and the honest count in the heading.
     *
     * <p>{@code page} is treated as an index a human typed: a value below one, an
     * unreadable value, or one beyond the last page all collapse to a real page
     * instead of an error, so a stale link or a hand-edited address still renders.
     * The row window is computed here, off the same total the feed of the current
     * page is counted from, so the pager never points past the end of the register.
     * The alternative - a raw {@code offset} travelling through the query string -
     * is a page number rendered as arithmetic, which is how a register drifts out
     * of step the moment any record is added.</p>
     */
    public PatientPage searchPage(ClinicPrincipal caller, String term, int page, int pageSize) {
        AccessControl.require(caller, Action.SEARCH_PATIENTS);
        String clean = (term == null || term.isBlank()) ? null : term.trim();
        long total = patients.countMatching(clean);
        int safePageSize = Math.max(1, pageSize);
        int totalPages = (int) Math.max(1, (total + safePageSize - 1) / safePageSize);
        int current = clamp(page, 1, totalPages);
        long offset = (long) (current - 1) * safePageSize;
        List<PatientResponse> rows = patients.page(clean, offset, safePageSize).stream()
                .map(PatientResponse::of)
                .toList();
        return new PatientPage(rows, total, current, totalPages, safePageSize);
    }

    /** One screenful of the register and what it sits inside. */
    public record PatientPage(List<PatientResponse> patients,
                              long total,
                              int page,
                              int totalPages,
                              int pageSize) {
        public long firstOnPage() {
            return total == 0 ? 0 : (long) (page - 1) * pageSize + 1;
        }

        public long lastOnPage() {
            return Math.min((long) page * pageSize, total);
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * One patient record.
     *
     * <p>A patient may read their own; staff who need it may read any. The
     * ownership case is checked against {@code userUid}, so a walk-in record - which
     * has none - is readable only by staff.</p>
     *
     * <p>Guarded by {@code READ_PATIENT_RECORD} rather than {@code SEARCH_PATIENTS},
     * because a dentist must be able to read the record of the patient in the chair
     * without being able to list everyone the clinic has treated.</p>
     */
    public PatientResponse findById(ClinicPrincipal caller, String id) {
        Patient patient = require(id);
        AccessControl.requireSelfOr(caller, patient.getUserUid(), Action.READ_PATIENT_RECORD);
        return PatientResponse.of(patient);
    }

    /**
     * The record belonging to a signed-in patient, used wherever a screen needs
     * "my" details rather than a chosen id.
     */
    public Optional<PatientResponse> findOwn(ClinicPrincipal caller) {
        if (caller == null) {
            return Optional.empty();
        }
        return patients.findByUserUid(caller.uid()).map(PatientResponse::of);
    }

    /**
     * Other patients reachable on the same contact number as {@code id}.
     *
     * <p>Exists so the register screen can show the duplicate warning after a
     * redirect-after-post, and still show it correctly if the page is refreshed. The
     * alternative - carrying a count in the query string - would survive a refresh
     * but stop being true the moment anyone edited a record.</p>
     */
    public List<PatientResponse> possibleDuplicatesOf(ClinicPrincipal caller, String id) {
        AccessControl.require(caller, Action.SEARCH_PATIENTS);
        Patient patient = require(id);
        return patients.findByContactNumber(patient.getContactNumber()).stream()
                .filter(other -> !other.getId().equals(id))
                .map(PatientResponse::of)
                .toList();
    }

    /**
     * Registers a walk-in: a patient the clinic treats who has no portal account
     * (FR-REC-21, ASM-04).
     *
     * <p>Two defects in the previous version are fixed here, and both were about the
     * same line of code.</p>
     *
     * <p><b>It required no permission.</b> {@code doPost} called straight through
     * without asking {@link AccessControl}, so any signed-in caller could create a
     * patient record - verified by doing it as the seeded patient. Four lines away
     * in the same class, the search route did check. It read as an omission.</p>
     *
     * <p><b>It inherited the caller's account.</b> The record was saved with
     * {@code userUid} set to whoever was signed in, so a patient calling it gained a
     * second profile row against their own uid, and
     * {@code findByUserUid} - which patient booking resolves through - became
     * ambiguous. When reception called it, the walk-in was linked to the
     * <em>receptionist's</em> account, and reported as having a portal account they
     * had never had. A walk-in's {@code userUid} is null, always; the only thing
     * that sets it is patient self-registration, which belongs to
     * {@code UserAccountFactory} because that is what owns account creation.</p>
     *
     * @return the created record, and any existing patients on the same contact
     *         number so the caller can warn about a probable duplicate (FR-REC-25)
     */
    public Registration register(ClinicPrincipal caller, NewPatient details) {
        AccessControl.require(caller, Action.REGISTER_PATIENT);

        String name = required(details.name(), "name");
        String contactNumber = validPhone(details.contactNumber());
        String email = validEmail(details.email());
        LocalDate dob = parseDob(details.dob());

        List<PatientResponse> duplicates = patients.findByContactNumber(contactNumber).stream()
                .map(PatientResponse::of).toList();

        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                // Never the caller's uid. A walk-in has no portal account.
                .userUid(null)
                .name(name)
                .address(trimToNull(details.address()))
                .contactNumber(contactNumber)
                .email(email)
                .dob(dob)
                .build();
        patients.save(patient);

        log.log(Level.INFO, "patient_registered id={0} by={1} duplicates={2}",
                new Object[] { patient.getId(), caller.uid(), duplicates.size() });

        return new Registration(PatientResponse.of(patient), duplicates);
    }

    /** What the caller supplies to register someone. */
    public record NewPatient(String name,
                             String contactNumber,
                             String address,
                             String email,
                             String dob) {
    }

    /** What a patient may change about their own record. Email is deliberately absent:
     *  it is the identity that signs them in, so it is not editable here.
     *  GAP-PAT-27 adds the patient's own diagnosis / history details. */
    public record ProfileUpdate(String name,
                                String address,
                                String contactNumber,
                                String dob,
                                String diagnosisDetails) {
    }

    /**
     * Edits a patient's own contact details.
     *
     * <p>Email is never updated: it is the login identity, and letting it move
     * would let an account be quietly retargeted. The record is resolved from the
     * caller's own uid, so there is no id parameter to change to reach someone
     * else's record.</p>
     */
    public PatientResponse updateOwn(ClinicPrincipal caller, ProfileUpdate details) {
        AccessControl.require(caller, Action.EDIT_OWN_PROFILE);

        Patient patient = patients.findByUserUid(caller.uid())
                .orElseThrow(() -> new ResourceNotFoundException("No profile for this account"));

        String name = required(details.name(), "name");
        String contactNumber = validPhone(details.contactNumber());
        LocalDate dob = parseDob(details.dob());

        Patient updated = Patient.builder()
                .id(patient.getId())
                .userUid(patient.getUserUid())
                .name(name)
                .address(trimToNull(details.address()))
                .contactNumber(contactNumber)
                .email(patient.getEmail())
                .dob(dob)
                .diagnosisDetails(trimToNull(details.diagnosisDetails()))
                .build();
        patients.save(updated);

        log.log(Level.INFO, "patient_profile_updated id={0} by={1}",
                new Object[] { patient.getId(), caller.uid() });

        return PatientResponse.of(updated);
    }

    /**
     * The outcome of a registration.
     *
     * <p>{@code possibleDuplicates} is a warning, not a refusal. Two people can
     * share a telephone number - a household, a parent booking for a child - so
     * rejecting the second would block a legitimate registration. FR-REC-25 asks
     * for a warning, and the screen shows it beside the record it just created.</p>
     */
    public record Registration(PatientResponse patient, List<PatientResponse> possibleDuplicates) {

        public boolean hasPossibleDuplicates() {
            return !possibleDuplicates.isEmpty();
        }
    }

    private Patient require(String id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String validEmail(String email) {
        String trimmed = trimToNull(email);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("email must be valid");
        }
        return trimmed;
    }

    /** Required, and must be a number the clinic would store. */
    private static String validPhone(String value) {
        return PhoneNumbers.validate(required(value, "contactNumber"), "contactNumber");
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
            throw new IllegalArgumentException("dob must be a date in yyyy-MM-dd format");
        }
        if (parsed.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dob cannot be in the future");
        }
        return parsed;
    }
}
