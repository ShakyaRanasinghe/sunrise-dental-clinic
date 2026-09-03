package com.sunrise.clinic.patients;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.patients.data.InMemoryPatientRepository;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientResponse;
import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.patients.service.PatientService.NewPatient;
import com.sunrise.clinic.patients.service.PatientService.Registration;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 3a, alongside the service it tests. In {@code layered/} there was no
 * patient service and therefore nothing to test at this level - the logic lived
 * inline in two servlets, twice, and the two copies disagreed.
 */
class PatientServiceTest {

    private static final ClinicPrincipal PATIENT =
            new ClinicPrincipal("u-pat1", "Nimal Perera", Role.PATIENT);
    private static final ClinicPrincipal RECEPTION =
            new ClinicPrincipal("u-recep", "Kumari Silva", Role.RECEPTIONIST);
    private static final ClinicPrincipal DENTIST =
            new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);
    private static final ClinicPrincipal ADMIN =
            new ClinicPrincipal("u-admin", "Anoma Fernando", Role.ADMIN);

    private PatientRepository patients;
    private PatientService service;

    @BeforeEach
    void setUp() {
        patients = new InMemoryPatientRepository();
        patients.save(Patient.builder()
                .id("p-nimal").userUid("u-pat1").name("Nimal Perera")
                .contactNumber("0771234567").email("nimal@example.lk")
                .dob(LocalDate.of(1988, 4, 12)).build());
        patients.save(Patient.builder()
                .id("p-arun").name("Arun Wickrama")
                .contactNumber("0712223334").build());
        service = new PatientService(patients);
    }

    // --- the two defects this step exists to fix ----------------------

    @Test
    void aPatientCannotRegisterAPatient() {
        // The first defect. doPost called register with no AccessControl check at
        // all, so any signed-in caller could create a record - verified against the
        // running application as the seeded patient, which answered 201.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.register(PATIENT, walkIn()));
    }

    @Test
    void aDentistCannotRegisterAPatient() {
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.register(DENTIST, walkIn()));
    }

    @Test
    void receptionAndAdminCanRegisterAPatient() {
        assertEquals("Sunil Bandara", service.register(RECEPTION, walkIn()).patient().name());
        assertEquals("Sunil Bandara", service.register(ADMIN, walkIn()).patient().name());
    }

    @Test
    void aRegisteredWalkInIsNotLinkedToWhoeverRegisteredThem() {
        // The second defect, and the more damaging one. register() set
        // userUid to the caller's own uid, so a walk-in registered by reception was
        // linked to the receptionist's account and reported as having a portal
        // account they had never had - and a patient calling it gave themselves a
        // second profile row, which made findByUserUid ambiguous. Patient booking
        // resolves patientId through exactly that call.
        Registration created = service.register(RECEPTION, walkIn());

        Patient stored = patients.findById(created.patient().id()).orElseThrow();
        assertNull(stored.getUserUid(), "a walk-in has no portal account");
        assertFalse(created.patient().hasPortalAccount());
    }

    @Test
    void registeringDoesNotGiveTheReceptionistASecondPatientRow() {
        service.register(RECEPTION, walkIn());

        assertTrue(patients.findByUserUid("u-recep").isEmpty(),
                "the receptionist must not acquire a patient record by doing their job");
    }

    // --- search -------------------------------------------------------

    @Test
    void searchMatchesNameContactNumberAndEmail() {
        assertEquals(List.of("Nimal Perera"), names(service.search(RECEPTION, "perera")));
        assertEquals(List.of("Nimal Perera"), names(service.search(RECEPTION, "0771234")));
        assertEquals(List.of("Nimal Perera"), names(service.search(RECEPTION, "nimal@")));
    }

    @Test
    void aBlankTermListsEveryone() {
        // What the screen does when it opens, so it must not mean "match nothing".
        assertEquals(2, service.search(RECEPTION, null).size());
        assertEquals(2, service.search(RECEPTION, "   ").size());
    }

    @Test
    void aPatientCannotListTheRegister() {
        // Every patient the clinic has ever treated, with contact details, is not a
        // patient's to read.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.search(PATIENT, ""));
    }

    @Test
    void aDentistCannotListTheRegisterEither() {
        // But a dentist can read one record - see below. That is why searching and
        // reading are separate actions.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.search(DENTIST, ""));
    }

    // --- pagination ----------------------------------------------------

    @Test
    void theRegisterIsHandedOutInPages() {
        register("p-bimal", "Bimal Jayasuriya", "0771112221");
        register("p-chamath", "Chamath Silva", "0771112222");
        register("p-dinithi", "Dinithi Perera", "0771112223");

        PatientService.PatientPage first = service.searchPage(RECEPTION, null, 1, 2);
        assertEquals(List.of("Arun Wickrama", "Bimal Jayasuriya"), names(first.patients()));
        assertEquals(5, first.total());
        assertEquals(1, first.page());
        assertEquals(3, first.totalPages());
        assertEquals(1, first.firstOnPage());
        assertEquals(2, first.lastOnPage());

        PatientService.PatientPage last = service.searchPage(RECEPTION, null, 3, 2);
        assertEquals(List.of("Nimal Perera"), names(last.patients()));
        // The honest record count, not the number of rows on this page - the JSP
        // prints this in the heading above the pager.
        assertEquals(5, last.total());
        assertEquals(3, last.page());
        assertEquals(5, last.firstOnPage());
        assertEquals(5, last.lastOnPage());
    }

    @Test
    void aPageNumberTooBigOrTooSmallCollapsesToARealPage() {
        register("p-bimal", "Bimal Jayasuriya", "0771112221");

        // A stale link or a hand-edited ?page= must render, not error (there are
        // four patients now; the last page is 2).
        assertEquals(2, service.searchPage(RECEPTION, null, 99, 2).page());
        assertEquals(2, service.searchPage(RECEPTION, null, Integer.MAX_VALUE, 2).page());
        assertEquals(1, service.searchPage(RECEPTION, null, 0, 2).page());
        assertEquals(1, service.searchPage(RECEPTION, null, -4, 2).page());
    }

    @Test
    void aSearchResultIsPagedWithItsOwnTotal() {
        register("p-dinithi", "Dinithi Perera", "0771112223");

        PatientService.PatientPage first = service.searchPage(RECEPTION, "perera", 1, 1);
        assertEquals(List.of("Dinithi Perera"), names(first.patients()));
        assertEquals(2, first.total());
        assertEquals(1, first.firstOnPage());
        assertEquals(1, first.lastOnPage());

        PatientService.PatientPage second = service.searchPage(RECEPTION, "perera", 2, 1);
        assertEquals(List.of("Nimal Perera"), names(second.patients()));
        assertEquals(2, second.total());
        assertEquals(2, second.page());
        assertEquals(2, second.firstOnPage());
        assertEquals(2, second.lastOnPage());
    }

    @Test
    void pagingTheRegisterAlsoRequiresSearchPermission() {
        // Pagination is a different way of reading the same list, not a new read.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.searchPage(PATIENT, null, 1, 20));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.searchPage(DENTIST, null, 1, 20));
    }

    // --- reading one record -------------------------------------------

    @Test
    void aDentistMayReadOnePatientRecord() {
        assertEquals("Nimal Perera", service.findById(DENTIST, "p-nimal").name());
    }

    @Test
    void aPatientMayReadTheirOwnRecord() {
        assertEquals("Nimal Perera", service.findById(PATIENT, "p-nimal").name());
    }

    @Test
    void aPatientMayNotReadSomeoneElsesRecord() {
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.findById(PATIENT, "p-arun"));
    }

    @Test
    void aWalkInRecordIsReadableOnlyByStaff() {
        // It has no userUid, so the ownership branch can never match it. Without the
        // action check a null owner and a null caller would compare equal.
        assertEquals("Arun Wickrama", service.findById(RECEPTION, "p-arun").name());
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.findById(null, "p-arun"));
    }

    @Test
    void anUnknownIdIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.findById(RECEPTION, "p-nobody"));
    }

    @Test
    void findOwnResolvesTheCallersOwnRecord() {
        assertEquals("Nimal Perera", service.findOwn(PATIENT).orElseThrow().name());
        assertTrue(service.findOwn(RECEPTION).isEmpty(), "staff have no patient record");
    }

    // --- validation ---------------------------------------------------

    @Test
    void nameAndContactNumberAreRequired() {
        assertEquals("name is required", assertThrows(IllegalArgumentException.class,
                () -> service.register(RECEPTION,
                        new NewPatient("  ", "0761112223", null, null, null))).getMessage());
        assertEquals("contactNumber is required", assertThrows(IllegalArgumentException.class,
                () -> service.register(RECEPTION,
                        new NewPatient("Sunil Bandara", null, null, null, null))).getMessage());
    }

    @Test
    void addressEmailAndDobAreOptional() {
        PatientResponse created = service.register(RECEPTION,
                new NewPatient("Sunil Bandara", "0761112223", null, null, null)).patient();

        assertNull(created.address());
        assertNull(created.email());
        assertNull(created.dob());
    }

    // --- the phone number is validated like self-registration's ---------

    @Test
    void aContactNumberWithLettersIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "o77 123 4567", null, null, null)));
    }

    @Test
    void aContactNumberMustStartWithZero() {
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "7112345678", null, null, null)));
    }

    @Test
    void aContactNumberWithTheWrongDigitCountIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "071234", null, null, null)));
    }

    @Test
    void updateOwnAlsoRefusesAMalformedPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () -> service.updateOwn(PATIENT,
                new PatientService.ProfileUpdate("Nimal Perera", null, "o77 123 4567", null, null)));
    }

    @Test
    void aMalformedEmailIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "0761112223", null, "not-an-address", null)));
    }

    @Test
    void aDobInTheFutureIsRejected() {
        // A patient cannot be born tomorrow, and the commonest cause is a mistyped
        // year at the desk.
        String tomorrow = LocalDate.now().plusDays(1).toString();
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "0761112223", null, null, tomorrow)));
    }

    @Test
    void aMalformedDobIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.register(RECEPTION,
                new NewPatient("Sunil", "0761112223", null, null, "12/04/1988")));
    }

    @Test
    void blankOptionalFieldsBecomeNullRatherThanEmptyStrings() {
        // An HTML form posts "" for an untouched field. Storing that would make
        // "no email" and "empty email" two different states in the database.
        PatientResponse created = service.register(RECEPTION,
                new NewPatient("Sunil", "0761112223", "  ", "", "  ")).patient();

        assertNull(created.address());
        assertNull(created.email());
        assertNull(created.dob());
    }

    // --- the duplicate warning, FR-REC-25 -----------------------------

    @Test
    void registeringOnAnExistingContactNumberWarnsButSucceeds() {
        Registration created = service.register(RECEPTION,
                new NewPatient("Nimal Perera Jr", "0771234567", null, null, null));

        assertTrue(created.hasPossibleDuplicates());
        assertEquals(List.of("Nimal Perera"), names(created.possibleDuplicates()));
        // A warning, not a refusal: a household shares a number, and a parent books
        // for a child. Refusing would block a legitimate registration.
        assertTrue(patients.findById(created.patient().id()).isPresent());
    }

    @Test
    void aFreshContactNumberWarnsAboutNothing() {
        assertFalse(service.register(RECEPTION, walkIn()).hasPossibleDuplicates());
    }

    @Test
    void theDuplicateQueryExcludesTheRecordItself() {
        assertEquals(List.of(), service.possibleDuplicatesOf(RECEPTION, "p-nimal"));

        service.register(RECEPTION, new NewPatient("Nimal Perera Jr", "0771234567", null, null, null));

        assertEquals(List.of("Nimal Perera Jr"), names(service.possibleDuplicatesOf(RECEPTION, "p-nimal")));
    }

    // --- what leaves the module ---------------------------------------

    @Test
    void theResponseNeverCarriesTheAccountIdentifier() {
        // PatientResponse exists so that userUid does not reach a screen: an internal
        // identifier on a page invites its use as a parameter. hasPortalAccount is
        // what reception actually needs (FR-REC-22).
        PatientResponse response = service.findById(RECEPTION, "p-nimal");

        assertTrue(response.hasPortalAccount());
        assertFalse(List.of(PatientResponse.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getName().toLowerCase().contains("uid")),
                "PatientResponse must not expose a uid");
    }

    private static NewPatient walkIn() {
        return new NewPatient("Sunil Bandara", "0761112223", "5 Lake Road, Kandy", null, null);
    }

    private void register(String id, String name, String contactNumber) {
        patients.save(Patient.builder()
                .id(id).name(name).contactNumber(contactNumber).build());
    }

    private static List<String> names(List<PatientResponse> found) {
        return found.stream().map(PatientResponse::name).toList();
    }
}
