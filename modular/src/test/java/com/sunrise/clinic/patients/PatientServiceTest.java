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

    // --- correcting details, FR-REC-24 --------------------------------

    @Test
    void aMistypedNumberCanBeCorrected() {
        // The oldest outstanding item on the list. Until this existed the number stayed wrong,
        // and the only way round it was a second record for the same person.
        PatientResponse corrected = service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771230000", "14 Galle Road",
                        "nimal@example.lk", "1988-04-12")).patient();

        assertEquals("0771230000", corrected.contactNumber());
        assertEquals("0771230000", patients.findById("p-nimal").orElseThrow().getContactNumber());
    }

    @Test
    void correctingNeverTouchesTheAccountLink() {
        // The important one. A userUid reception can set is a way to attach a patient record to
        // somebody else's account - and findByUserUid, which patient booking resolves through,
        // would then answer with the wrong person. It is the same defect this module opened
        // with, arriving by a different door.
        service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771230000", null, null, null));

        assertEquals("u-pat1", patients.findById("p-nimal").orElseThrow().getUserUid());
        assertEquals(1, patients.findAll().stream()
                .filter(p -> "u-pat1".equals(p.getUserUid())).count(),
                "and no second record acquires the link");
    }

    @Test
    void correctingAWalkInLeavesThemAWalkIn() {
        service.correct(RECEPTION, "p-arun",
                new NewPatient("Arun Wickrama", "0712223999", null, null, null));

        assertNull(patients.findById("p-arun").orElseThrow().getUserUid());
        assertFalse(service.findById(RECEPTION, "p-arun").hasPortalAccount());
    }

    @Test
    void theAdministratorMayCorrectToo() {
        assertEquals("0771230000", service.correct(ADMIN, "p-nimal",
                new NewPatient("Nimal Perera", "0771230000", null, null, null))
                .patient().contactNumber());
    }

    @Test
    void neitherAPatientNorADentistMayCorrectARecord() {
        for (ClinicPrincipal caller : List.of(PATIENT, DENTIST)) {
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> service.correct(caller, "p-nimal",
                            new NewPatient("Changed Name", "0000000000", null, null, null)));
        }
        assertEquals("Nimal Perera", patients.findById("p-nimal").orElseThrow().getName());
    }

    @Test
    void correctingIntoAnExistingNumberWarnsWithoutRefusing()
    {
        // The same rule as registering: a household shares a number, so this is advice.
        var corrected = service.correct(RECEPTION, "p-arun",
                new NewPatient("Arun Wickrama", "0771234567", null, null, null));

        assertTrue(corrected.hasPossibleDuplicates());
        assertEquals(List.of("Nimal Perera"), names(corrected.possibleDuplicates()));
    }

    @Test
    void aCorrectedRecordIsNotItsOwnDuplicate() {
        var corrected = service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771234567", null, null, null));

        assertFalse(corrected.hasPossibleDuplicates(), "the record itself is excluded");
    }

    @Test
    void correctingRefusesTheSameThingsRegisteringDoes() {
        assertThrows(IllegalArgumentException.class, () -> service.correct(RECEPTION, "p-nimal",
                new NewPatient("  ", "0771234567", null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771234567", null, "not-an-address", null)));
        assertThrows(IllegalArgumentException.class, () -> service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771234567", null, null,
                        LocalDate.now().plusDays(1).toString())));
    }

    @Test
    void correctingAnUnknownPatientIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.correct(RECEPTION, "p-nobody",
                new NewPatient("Somebody", "0771234567", null, null, null)));
    }

    @Test
    void blankOptionalFieldsAreClearedRatherThanKept() {
        // Correcting a record means the form is authoritative. Somebody who deletes a wrong
        // address expects it gone, not silently retained.
        service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771234567", "14 Galle Road", null, null));
        service.correct(RECEPTION, "p-nimal",
                new NewPatient("Nimal Perera", "0771234567", "  ", null, null));

        assertNull(patients.findById("p-nimal").orElseThrow().getAddress());
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

    private static List<String> names(List<PatientResponse> found) {
        return found.stream().map(PatientResponse::name).toList();
    }
}
