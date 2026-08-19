package com.sunrise.clinic.patients;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.patients.data.InMemoryPatientNoteRepository;
import com.sunrise.clinic.patients.data.InMemoryPatientRepository;
import com.sunrise.clinic.patients.domain.NoteCategory;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientNoteResponse;
import com.sunrise.clinic.patients.service.PatientNoteService;
import com.sunrise.clinic.patients.service.TreatmentRelationship;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 8a. The three confidentiality rules are the point of this class, so most of
 * what follows is about who may see what rather than about storing a note.
 */
class PatientNoteServiceTest {

    private static final ClinicPrincipal NIMAL =
            new ClinicPrincipal("u-pat1", "Nimal Perera", Role.PATIENT);
    private static final ClinicPrincipal KAMALA =
            new ClinicPrincipal("u-pat2", "Kamala Fernando", Role.PATIENT);
    private static final ClinicPrincipal SILVA =
            new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);
    private static final ClinicPrincipal JAYASURIYA =
            new ClinicPrincipal("u-dent2", "Dr. Malini Jayasuriya", Role.DENTIST);
    private static final ClinicPrincipal RECEPTION =
            new ClinicPrincipal("u-recep", "Kumari Silva", Role.RECEPTIONIST);
    private static final ClinicPrincipal ADMIN =
            new ClinicPrincipal("u-admin", "Anoma Fernando", Role.ADMIN);

    private InMemoryPatientNoteRepository notes;
    private PatientNoteService service;

    @BeforeEach
    void setUp() {
        notes = new InMemoryPatientNoteRepository();
        InMemoryPatientRepository patients = new InMemoryPatientRepository();
        patients.save(Patient.builder().id("p-nimal").userUid("u-pat1").name("Nimal Perera").build());
        patients.save(Patient.builder().id("p-kamala").userUid("u-pat2").name("Kamala Fernando").build());

        // Dr. Silva treats Nimal. Dr. Jayasuriya treats nobody in these tests.
        TreatmentRelationship treating = (dentistUid, patientId) ->
                "u-dent1".equals(dentistUid) && "p-nimal".equals(patientId);
        service = new PatientNoteService(notes, patients, treating);
    }

    // --- the patient owns their own record -----------------------------

    @Test
    void aPatientDeclaresAndReadsTheirOwnNotes() {
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        List<PatientNoteResponse> own = service.own(NIMAL);

        assertEquals(1, own.size());
        assertEquals("Allergic to penicillin", own.get(0).detail());
        assertEquals("Allergy", own.get(0).categoryLabel());
        assertTrue(own.get(0).critical());
    }

    @Test
    void aPatientCanCorrectAndWithdrawTheirOwnNote() {
        // FR-NOTE-05. A medical fact that has changed and cannot be corrected is worse than
        // no record - the dentist reads a warning that has ceased to apply.
        String id = service.declare(NIMAL, NoteCategory.MEDICATION, "Taking warfarin", true).id();

        service.amend(NIMAL, id, NoteCategory.MEDICATION, "No longer taking warfarin", false);
        assertEquals("No longer taking warfarin", service.own(NIMAL).get(0).detail());
        assertFalse(service.own(NIMAL).get(0).critical());

        service.withdraw(NIMAL, id);
        assertEquals(List.of(), service.own(NIMAL));
    }

    @Test
    void aPatientCannotTouchAnotherPatientsNote() {
        String id = service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to latex", true).id();

        // Not found rather than forbidden: confirming it exists would tell Kamala that
        // some other patient has declared something.
        assertThrows(ResourceNotFoundException.class,
                () -> service.amend(KAMALA, id, NoteCategory.OTHER, "changed", false));
        assertThrows(ResourceNotFoundException.class, () -> service.withdraw(KAMALA, id));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.forPatient(KAMALA, "p-nimal"));
    }

    // --- the dentist reads, and cannot write --------------------------

    @Test
    void theTreatingDentistReadsTheNotes() {
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        assertEquals(1, service.forPatient(SILVA, "p-nimal").size());
        assertTrue(service.hasCriticalNotes(SILVA, "p-nimal"));
    }

    @Test
    void aDentistNotTreatingThemCannotRead() {
        // FR-NOTE-09. Holding READ_CLINICAL makes someone clinical staff, not this
        // patient's dentist.
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.forPatient(JAYASURIYA, "p-nimal"));
    }

    @Test
    void noDentistCanDeclareAmendOrWithdraw() {
        // FR-NOTE-10. The patient owns their own record; a dentist who disagrees records
        // that as a diagnosis, not by editing what the patient said.
        String id = service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true).id();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.declare(SILVA, NoteCategory.OTHER, "Patient seems fine", false));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.amend(SILVA, id, NoteCategory.OTHER, "Not really allergic", false));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> service.withdraw(SILVA, id));
        assertEquals("Allergic to penicillin", service.own(NIMAL).get(0).detail());
    }

    // --- reception and the administrator see nothing at all ------------

    @Test
    void neitherReceptionNorTheAdministratorCanRead() {
        // FR-NOTE-11. Not a redacted note - nothing.
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        for (ClinicPrincipal caller : List.of(RECEPTION, ADMIN)) {
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> service.forPatient(caller, "p-nimal"), caller.role() + " must see nothing");
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> service.hasCriticalNotes(caller, "p-nimal"));
        }
    }

    @Test
    void anAnonymousCallerIsNotAuthenticated() {
        assertThrows(AccessControl.NotAuthenticatedException.class,
                () -> service.forPatient(null, "p-nimal"));
    }

    @Test
    void theResponseCarriesNoPatientIdentifier() {
        // A note only ever reaches somebody already entitled to it, so it does not need to
        // say whose it is - and a screen that has the id is a screen that can be made to
        // ask for a different one.
        assertFalse(Set.of(PatientNoteResponse.class.getRecordComponents()).stream()
                        .anyMatch(c -> c.getName().toLowerCase().contains("patient")),
                "PatientNoteResponse must not expose a patient id");
    }

    // --- the order matters --------------------------------------------

    @Test
    void criticalNotesComeFirst() {
        // FR-NOTE-08. A list that puts the allergy third is a list that gets skimmed past.
        service.declare(NIMAL, NoteCategory.OTHER, "Nervous about needles", false);
        service.declare(NIMAL, NoteCategory.CONDITION, "Type 2 diabetes", false);
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        List<PatientNoteResponse> own = service.own(NIMAL);

        assertEquals(3, own.size());
        assertTrue(own.get(0).critical(), "the important one must be first");
        assertEquals("Allergic to penicillin", own.get(0).detail());
    }

    // --- the empty state is a real answer ------------------------------

    @Test
    void aPatientWithNothingDeclaredReturnsAnEmptyListNotAFailure() {
        // FR-NOTE-12. The caller renders "nothing declared"; a blank would look like a
        // screen that failed to load, and a dentist cannot tell the difference.
        assertEquals(List.of(), service.forPatient(SILVA, "p-nimal"));
        assertFalse(service.hasCriticalNotes(SILVA, "p-nimal"));
    }

    @Test
    void nothingCriticalMeansNoBanner() {
        service.declare(NIMAL, NoteCategory.OTHER, "Nervous about needles", false);

        assertFalse(service.hasCriticalNotes(SILVA, "p-nimal"));
    }

    // --- validation ----------------------------------------------------

    @Test
    void aNoteNeedsACategoryAndSomeDetail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.declare(NIMAL, null, "Something", false));
        assertThrows(IllegalArgumentException.class,
                () -> service.declare(NIMAL, NoteCategory.OTHER, "  ", false));
        assertThrows(IllegalArgumentException.class,
                () -> service.declare(NIMAL, NoteCategory.OTHER, "ab", false));
    }

    @Test
    void anOverlongNoteIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> service.declare(NIMAL, NoteCategory.OTHER, "x".repeat(1001), false));
    }

    @Test
    void aPatientWithNoRecordIsToldSo() {
        ClinicPrincipal ghost = new ClinicPrincipal("u-ghost", "Ghost", Role.PATIENT);

        assertThrows(ResourceNotFoundException.class,
                () -> service.declare(ghost, NoteCategory.OTHER, "Something", false));
    }

    @Test
    void correctingANoteKeepsTheDayItWasFirstDeclared() {
        // A corrected note is still one the patient declared on the day they declared it.
        String id = service.declare(NIMAL, NoteCategory.MEDICATION, "Taking warfarin", true).id();
        var createdAt = notes.findById(id).orElseThrow().getCreatedAt();

        service.amend(NIMAL, id, NoteCategory.MEDICATION, "Taking apixaban", true);

        assertEquals(createdAt, notes.findById(id).orElseThrow().getCreatedAt());
    }

    @Test
    void theDetailNeverAppearsInToString() {
        // toString ends up in logs and exception messages, and the detail is the one field
        // here that is medical information about a named person.
        service.declare(NIMAL, NoteCategory.ALLERGY, "Allergic to penicillin", true);

        assertFalse(notes.findAll().get(0).toString().contains("penicillin"),
                notes.findAll().get(0).toString());
    }
}
