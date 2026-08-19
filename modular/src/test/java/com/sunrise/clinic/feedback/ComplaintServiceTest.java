package com.sunrise.clinic.feedback;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.feedback.domain.ComplaintCategory;
import com.sunrise.clinic.feedback.domain.ComplaintResponse;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** New in step 8b. Mostly about who may see a complaint, which is the point of the feature. */
class ComplaintServiceTest {

    private static final String DETAIL =
            "I waited over an hour past my appointment time and nobody explained why.";

    private FeedbackTestFixture fixture;
    private ClinicPrincipal nimal;
    private ClinicPrincipal kamala;
    private String visit;

    @BeforeEach
    void setUp() {
        fixture = new FeedbackTestFixture(LocalDate.of(2026, 8, 20));
        nimal = fixture.appointments.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        kamala = fixture.appointments.addPatient("p-kamala", "u-pat2", "Kamala Fernando");
        visit = fixture.treatedVisit(nimal, "s1", fixture.today().minusDays(2));
    }

    private ComplaintResponse raise() {
        return fixture.complaintService.raise(nimal, "d-silva", visit,
                ComplaintCategory.WAIT_TIME, DETAIL);
    }

    // --- raising -------------------------------------------------------

    @Test
    void aPatientRaisesAConcernAboutADentistWhoTreatedThem() {
        ComplaintResponse raised = raise();

        assertEquals(ComplaintStatus.SUBMITTED, raised.status());
        assertEquals("Dr. Ranil Silva", raised.dentistName());
        assertEquals("Nimal Perera", raised.patientName());
        assertEquals(DETAIL, raised.detail());
        assertTrue(raised.isOpen());
    }

    @Test
    void aConcernCannotNameADentistWhoNeverTreatedThem() {
        // A complaints channel open against anybody is a channel for abuse rather than for
        // concerns.
        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.raise(
                nimal, "d-jaya", null, ComplaintCategory.CONDUCT, DETAIL));
    }

    @Test
    void aConcernCannotNameSomeoneElsesAppointment() {
        String hers = fixture.treatedVisit(kamala, "s2", fixture.today().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.raise(
                nimal, "d-silva", hers, ComplaintCategory.CONDUCT, DETAIL));
    }

    @Test
    void theAppointmentIsOptional() {
        // FR-CMP-03 says "should be linkable", not "must be linked". A concern about how
        // somebody was spoken to on the telephone has no appointment.
        assertEquals(ComplaintStatus.SUBMITTED, fixture.complaintService.raise(
                nimal, "d-silva", null, ComplaintCategory.CONDUCT, DETAIL).status());
    }

    @Test
    void aConcernNeedsEnoughDetailToActOn() {
        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.raise(
                nimal, "d-silva", visit, ComplaintCategory.CONDUCT, "bad"));
        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.raise(
                nimal, "d-silva", visit, null, DETAIL));
    }

    @Test
    void onlyAPatientCanRaiseOne() {
        for (ClinicPrincipal caller : List.of(AppointmentTestFixture.reception(),
                AppointmentTestFixture.silva(), AppointmentTestFixture.admin())) {
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> fixture.complaintService.raise(caller, "d-silva", null,
                            ComplaintCategory.CONDUCT, DETAIL));
        }
    }

    // --- the dentist never sees it, and nor does reception -------------

    @Test
    void theDentistNamedInItCannotReachItByAnyRoute() {
        // FR-CMP-08. Not the complaint, not its existence, not a count.
        ComplaintResponse raised = raise();
        ClinicPrincipal silva = AppointmentTestFixture.silva();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.read(silva, raised.id()));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.search(silva, null, null, null, null));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.countByDentist(silva));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.own(silva));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.beginReview(silva, raised.id()));
    }

    @Test
    void aReceptionistCannotReachItEither() {
        // FR-CMP-09.
        ComplaintResponse raised = raise();
        ClinicPrincipal reception = AppointmentTestFixture.reception();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.read(reception, raised.id()));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.complaintService.search(reception, null, null, null, null));
    }

    @Test
    void anotherPatientCannotSeeIt() {
        raise();

        assertEquals(List.of(), fixture.complaintService.own(kamala));
    }

    @Test
    void thePatientSeesTheirOwnAndItsState() {
        // FR-CMP-04.
        ComplaintResponse raised = raise();
        fixture.complaintService.beginReview(AppointmentTestFixture.admin(), raised.id());

        List<ComplaintResponse> own = fixture.complaintService.own(nimal);

        assertEquals(1, own.size());
        assertEquals(ComplaintStatus.UNDER_REVIEW, own.get(0).status());
        assertEquals("Being looked at", own.get(0).statusLabel());
    }

    // --- the administrator's process -----------------------------------

    @Test
    void theStatusMachineIsEnforced() {
        // FR-CMP-05: somebody has to have looked at it. Straight from SUBMITTED to RESOLVED
        // would be a filing cabinet rather than a process.
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();

        assertThrows(IllegalStateException.class, () -> fixture.complaintService.close(
                admin, raised.id(), ComplaintStatus.RESOLVED, "We have spoken to the dentist."));

        fixture.complaintService.beginReview(admin, raised.id());
        assertEquals(ComplaintStatus.RESOLVED, fixture.complaintService.close(admin, raised.id(),
                ComplaintStatus.RESOLVED, "We have spoken to the dentist about it.").status());
    }

    @Test
    void aClosedComplaintCannotBeReopenedOrClosedAgain() {
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, raised.id());
        fixture.complaintService.close(admin, raised.id(), ComplaintStatus.DISMISSED,
                "The delay was caused by an emergency, and we explained this.");

        assertThrows(IllegalStateException.class,
                () -> fixture.complaintService.beginReview(admin, raised.id()));
        assertThrows(IllegalStateException.class, () -> fixture.complaintService.close(
                admin, raised.id(), ComplaintStatus.RESOLVED, "Changed my mind about it."));
    }

    @Test
    void closingRequiresAWrittenResolution() {
        // FR-ADM-53. A concern closed with no explanation is a concern ignored with extra
        // steps.
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, raised.id());

        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.close(
                admin, raised.id(), ComplaintStatus.RESOLVED, "  "));
        assertThrows(IllegalArgumentException.class, () -> fixture.complaintService.close(
                admin, raised.id(), ComplaintStatus.RESOLVED, "sorted"));
    }

    @Test
    void thePatientsAccountCannotBeChangedByAnybody() {
        // FR-CMP-06 and FR-ADM-55 together. There is no service method that writes detail
        // after the insert, and ComplaintDao's UPDATE statement cannot reach the column.
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, raised.id());
        fixture.complaintService.close(admin, raised.id(), ComplaintStatus.RESOLVED,
                "We have apologised and changed how we handle overruns.");

        assertEquals(DETAIL, fixture.complaintService.read(admin, raised.id()).detail());
    }

    @Test
    void theResolutionSitsBesideTheAccountNotOverIt() {
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, raised.id());
        ComplaintResponse closed = fixture.complaintService.close(admin, raised.id(),
                ComplaintStatus.RESOLVED, "We have apologised and changed our handover.");

        assertEquals(DETAIL, closed.detail());
        assertEquals("We have apologised and changed our handover.", closed.resolution());
    }

    @Test
    void openComplaintsComeFirst() {
        // FR-ADM-51. A list sorted by date alone puts one resolved this morning above one
        // submitted last week and still untouched.
        ComplaintResponse first = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, first.id());
        fixture.complaintService.close(admin, first.id(), ComplaintStatus.RESOLVED,
                "Dealt with by speaking to the dentist.");
        ComplaintResponse second = fixture.complaintService.raise(nimal, "d-silva", null,
                ComplaintCategory.CONDUCT, DETAIL);

        List<ComplaintResponse> all = fixture.complaintService.search(admin, null, null, null, null);

        assertEquals(second.id(), all.get(0).id(), "the open one must be first");
    }

    @Test
    void theListCanBeFilteredByStateAndDentist() {
        raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();

        assertEquals(1, fixture.complaintService
                .search(admin, ComplaintStatus.SUBMITTED, "d-silva", null, null).size());
        assertEquals(0, fixture.complaintService
                .search(admin, ComplaintStatus.RESOLVED, null, null, null).size());
        assertEquals(0, fixture.complaintService
                .search(admin, null, "d-jaya", null, null).size());
    }

    // --- auditing ------------------------------------------------------

    @Test
    void everyReadIsAudited() {
        // FR-CMP-11. Reading a person's account of something that upset them is itself an
        // act worth recording.
        ComplaintResponse raised = raise();
        long before = fixture.audit.count();

        fixture.complaintService.read(AppointmentTestFixture.admin(), raised.id());

        assertTrue(fixture.audit.count() > before);
        assertTrue(fixture.audit.findAll().stream()
                .anyMatch(e -> "COMPLAINT_READ".equals(e.getAction())
                        && "u-admin".equals(e.getActorUid())));
    }

    @Test
    void closingIsAuditedWithTheOutcome() {
        ComplaintResponse raised = raise();
        ClinicPrincipal admin = AppointmentTestFixture.admin();
        fixture.complaintService.beginReview(admin, raised.id());
        fixture.complaintService.close(admin, raised.id(), ComplaintStatus.DISMISSED,
                "No action needed; the delay was explained at the time.");

        assertTrue(fixture.audit.findAll().stream()
                .anyMatch(e -> "COMPLAINT_DISMISSED".equals(e.getAction())));
    }

    // --- what leaves the module ----------------------------------------

    @Test
    void theResponseCarriesNothingClinical() {
        // FR-ADM-58: a complaint must not expose medical notes or a diagnosis, even where
        // the complaint is about the clinical care. An administrator reviewing conduct is
        // not the patient's dentist.
        Set<String> fields = Set.of(ComplaintResponse.class.getRecordComponents()).stream()
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(fields.contains("diagnosis"), fields.toString());
        assertFalse(fields.stream().anyMatch(f -> f.toLowerCase().contains("note")), fields.toString());
    }

    @Test
    void theDetailNeverAppearsInToString() {
        raise();

        assertFalse(fixture.complaints.findAll().get(0).toString().contains("waited"),
                fixture.complaints.findAll().get(0).toString());
    }
}
