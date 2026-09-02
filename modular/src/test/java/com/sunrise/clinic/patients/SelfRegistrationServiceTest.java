package com.sunrise.clinic.patients;

import com.sunrise.clinic.access.data.InMemoryUserRepository;
import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.PasswordHasher;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.patients.data.InMemoryPatientRepository;
import com.sunrise.clinic.patients.service.SelfRegistrationService;
import com.sunrise.clinic.patients.service.SelfRegistrationService.Registered;
import com.sunrise.clinic.patients.service.SelfRegistrationService.Registration;
import com.sunrise.clinic.platform.data.SerialTransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New, after registration was found to be impossible.
 *
 * <p>Two defects sat here and neither had a test. The servlet required a
 * {@code contactNumber} the form never asked for, so every attempt answered
 * <b>400 "Contact number is required"</b>; and it created only the account, so anything that
 * did get through signed in to a profile that did not exist and failed at the first booking.</p>
 *
 * <p>The first was invisible to the unit tests because there was no service to test — the rule
 * lived in a servlet. That is the argument for this class existing at all.</p>
 */
class SelfRegistrationServiceTest {

    private UserRepository users;
    private InMemoryPatientRepository patients;
    private SelfRegistrationService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        patients = new InMemoryPatientRepository();
        service = new SelfRegistrationService(new UserAccountFactory(users), patients,
                new SerialTransactionRunner());
    }

    private static Registration minimal() {
        // The contact number is required since GAP-PAT-20, so the baseline fixture carries one.
        return new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "0771234567", null, null);
    }

    // --- the defect: a phone number is required -------------------------

    @Test
    void registeringNeedsAContactNumber() {
        assertEquals("Your contact number is required.", refusal(new Registration(
                "Nimal Perera", "nimal@example.lk", "Password123", "Password123",
                null, null, null)));
    }

    @Test
    void norAnAddressOrADateOfBirth() {
        // Address and DOB remain optional; only the contact number (GAP-PAT-20) is required.
        Registered registered = service.register(new Registration(
                "Nimal Perera", "nimal@example.lk", "Password123", "Password123",
                "0771234567", null, null));

        assertEquals("0771234567", registered.patient().contactNumber());
        assertNull(registered.patient().address());
        assertNull(registered.patient().dob());
    }

    @Test
    void butTheyArePersistedWhenGiven() {
        Registered registered = service.register(new Registration(
                "Nimal Perera", "nimal@example.lk", "Password123", "Password123",
                "0771234567", "14 Galle Road", "1988-04-12"));

        assertEquals("0771234567", registered.patient().contactNumber());
        assertEquals("14 Galle Road", registered.patient().address());
        assertEquals(LocalDate.of(1988, 4, 12), registered.patient().dob());
    }

    // --- the contact number must be digits, not letters -----------------

    @Test
    void aContactNumberWithLettersIsRefused() {
        assertTrue(refusal(new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "o77 123 4567", null, null))
                .contains("No letters"));
    }

    @Test
    void aContactNumberMustStartWithZero() {
        assertTrue(refusal(new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "7112345678", null, null))
                .contains("must start with 0"));
    }

    @Test
    void aContactNumberWithTheWrongDigitCountIsRefused() {
        assertTrue(refusal(new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "071234", null, null))
                .contains("10 digits"));
    }

    @Test
    void formattedDigitsAreAcceptedAndStoredAsGiven() {
        Registered registered = service.register(new Registration(
                "Nimal Perera", "nimal@example.lk", "Password123", "Password123",
                "+94 77 123 4567", null, null));

        assertEquals("+94 77 123 4567", registered.patient().contactNumber());
    }

    @Test
    void anInternationalFormWithTooFewDigitsIsRefused() {
        assertTrue(refusal(new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "+94 71 2345", null, null))
                .contains("must start with 0"));
    }

    @Test
    void aLocalFormWithTheWrongDigitCountIsRefused() {
        assertTrue(refusal(new Registration("Nimal Perera", "nimal@example.lk",
                "Password123", "Password123", "071234", null, null))
                .contains("10 digits"));
    }

    // --- the defect: both rows, linked ----------------------------------

    @Test
    void bothRecordsAreCreatedAndLinked() {
        // FR-PAT-04. The account alone signs in successfully and then fails at the first
        // thing it tries, which is worse than not registering at all.
        Registered registered = service.register(minimal());

        assertEquals(1, users.count());
        assertEquals(1, patients.count());
        assertEquals(registered.account().getUid(),
                patients.findAll().get(0).getUserUid(), "the profile must point at the account");
    }

    @Test
    void theProfileIsFindableByTheAccountThatOwnsIt() {
        // This is the lookup booking resolves through. If it returns nothing, a registered
        // patient cannot book.
        Registered registered = service.register(minimal());

        assertTrue(patients.findByUserUid(registered.account().getUid()).isPresent());
    }

    @Test
    void theAccountIsAPatientAndNothingElse() {
        // There is no role parameter to tamper with — the factory fixes it.
        assertEquals(Role.PATIENT, service.register(minimal()).account().getRole());
    }

    @Test
    void neitherRecordSurvivesAFailure() {
        // One transaction. A duplicate email fails inside it, and the patient row that the
        // same transaction would have written must not be left behind.
        service.register(minimal());

        assertThrows(IllegalArgumentException.class, () -> service.register(new Registration(
                "Somebody Else", "nimal@example.lk", "Password123", "Password123",
                "0777654321", null, null)));

        assertEquals(1, users.count());
        assertEquals(1, patients.count());
    }

    // --- what is required, and what it says -----------------------------

    @Test
    void theFourRealRequirementsAreNamedWhenMissing() {
        assertEquals("Your name is required.", refusal(
                new Registration("  ", "a@b.lk", "Password123", "Password123", null, null, null)));
        assertEquals("Your email address is required.", refusal(
                new Registration("A Name", "", "Password123", "Password123", null, null, null)));
        assertEquals("Please choose a password.", refusal(
                new Registration("A Name", "a@b.lk", "", "", null, null, null)));
    }

    @Test
    void aMistypedConfirmationIsCaught() {
        assertEquals("The two passwords do not match.", refusal(new Registration(
                "A Name", "a@b.lk", "Password123", "Password124", null, null, null)));
    }

    @Test
    void aShortPasswordIsRefusedWithTheLength() {
        assertTrue(refusal(new Registration("A Name", "a@b.lk", "short", "short",
                null, null, null)).contains("at least 8"));
    }

    @Test
    void aMalformedEmailIsRefused() {
        assertEquals("That does not look like an email address.", refusal(new Registration(
                "A Name", "not-an-address", "Password123", "Password123", null, null, null)));
    }

    @Test
    void aFutureDateOfBirthIsRefused() {
        assertTrue(refusal(new Registration("A Name", "a@b.lk", "Password123", "Password123",
                "0771234567", null, LocalDate.now().plusDays(1).toString())).contains("in the future"));
    }

    @Test
    void aMalformedDateOfBirthSaysTheFormatWanted() {
        assertTrue(refusal(new Registration("A Name", "a@b.lk", "Password123", "Password123",
                "0771234567", null, "12/04/1988")).contains("yyyy-MM-dd"));
    }

    @Test
    void theEmailIsStoredInLowerCase() {
        // So signing in is not case-sensitive by accident.
        assertEquals("nimal@example.lk", service.register(new Registration(
                "Nimal Perera", "  NIMAL@Example.LK  ", "Password123", "Password123",
                "0771234567", null, null)).patient().email());
    }

    @Test
    void onlyAHashOfThePasswordIsStored() {
        service.register(minimal());

        String stored = users.findAll().get(0).getPasswordHash();
        assertFalse(stored.contains("Password123"));
        assertTrue(PasswordHasher.matches("Password123", stored));
    }

    private String refusal(Registration form) {
        return assertThrows(IllegalArgumentException.class, () -> service.register(form))
                .getMessage();
    }
}
