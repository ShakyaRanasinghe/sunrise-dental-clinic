package com.sunrise.clinic.appointments;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.domain.AppointmentDetailResponse;
import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.appointments.service.AppointmentEvent;
import com.sunrise.clinic.appointments.service.AppointmentObserver;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.error.SlotUnavailableException;
import com.sunrise.clinic.scheduling.domain.SlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} in step 4 and extended for the three rules this step
 * introduced: ownership on {@code complete}, the status machine, and the list queries that
 * did not exist.
 */
class AppointmentServiceTest {

    private AppointmentTestFixture fixture;
    private ClinicPrincipal nimal;
    private ClinicPrincipal kamala;

    @BeforeEach
    void setUp() {
        fixture = new AppointmentTestFixture();
        nimal = fixture.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        kamala = fixture.addPatient("p-kamala", "u-pat2", "Kamala Fernando");
        fixture.addSlot("s1", LocalTime.of(16, 0));
        fixture.addSlot("s2", LocalTime.of(16, 30));
    }

    // --- booking ------------------------------------------------------

    @Test
    void aPatientBooksAnOpenSlot() {
        AppointmentResponse booked = fixture.service.book(nimal, "s1", "t-checkup", null);

        assertEquals("APT-20260720-0001", booked.appointmentNo());
        assertEquals(AppointmentStatus.CONFIRMED, booked.status());
        assertEquals("Nimal Perera", booked.patientName());
        assertEquals("Dr. Ranil Silva", booked.dentistName());
        assertEquals("Routine check-up", booked.treatmentName());
        assertEquals(SlotStatus.BOOKED, fixture.slots.findById("s1").orElseThrow().getStatus());
    }

    @Test
    void theSlotPointsBackAtTheAppointment() {
        // Both halves in one step, so a slot can never be BOOKED with no number - which
        // uq_slot_appointment would not catch, because NULL is not a duplicate.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertEquals(no, fixture.slots.findById("s1").orElseThrow().getAppointmentNo());
    }

    @Test
    void aPatientCannotBookInSomeoneElsesName() {
        // The id is resolved from the account, so a patientId in the request is ignored.
        AppointmentResponse booked = fixture.service.book(nimal, "s1", "t-checkup", "p-kamala");

        assertEquals("p-nimal", booked.patientId());
    }

    @Test
    void receptionMustNameThePatientItBooksFor() {
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.book(AppointmentTestFixture.reception(), "s1", "t-checkup", null));
    }

    @Test
    void receptionCanBookOnAPatientsBehalf() {
        AppointmentResponse booked = fixture.service.book(
                AppointmentTestFixture.reception(), "s1", "t-checkup", "p-nimal");

        assertEquals("p-nimal", booked.patientId());
    }

    @Test
    void aDentistDoesNotBook() {
        // Booking is the patient's or the front desk's job in this clinic.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.book(AppointmentTestFixture.silva(), "s1", "t-checkup", "p-nimal"));
    }

    @Test
    void bookingATakenSlotIsRefused() {
        fixture.service.book(nimal, "s1", "t-checkup", null);

        assertThrows(SlotUnavailableException.class,
                () -> fixture.service.book(kamala, "s1", "t-checkup", null));
        assertEquals(1, fixture.appointments.count());
    }

    @Test
    void anUnknownSlotOrTreatmentIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> fixture.service.book(nimal, "s-nobody", "t-checkup", null));
        // Checked before the transaction opens, so this is a clean 404 rather than a
        // foreign-key violation discovered halfway through booking.
        assertThrows(ResourceNotFoundException.class,
                () -> fixture.service.book(nimal, "s1", "t-nobody", null));
        assertEquals(0, fixture.appointments.count());
    }

    @Test
    void aPatientWithNoRecordCannotBook() {
        ClinicPrincipal ghost = new ClinicPrincipal("u-ghost", "Ghost",
                com.sunrise.clinic.access.domain.Role.PATIENT);

        assertThrows(ResourceNotFoundException.class,
                () -> fixture.service.book(ghost, "s1", "t-checkup", null));
    }

    @Test
    void appointmentNumbersRunSequentiallyPerDay() {
        assertEquals("APT-20260720-0001", fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo());
        assertEquals("APT-20260720-0002", fixture.service.book(kamala, "s2", "t-checkup", null).appointmentNo());
    }

    // --- the status machine -------------------------------------------

    @Test
    void aBilledAppointmentCannotBeCancelled() {
        // The defect that mattered financially: cancel() assigned CANCELLED whatever the
        // status was, releasing the slot while the bill stood.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Cleaned and polished");
        fixture.service.require(no).markBilled();
        fixture.appointments.save(fixture.service.require(no));

        assertThrows(IllegalStateException.class,
                () -> fixture.service.cancel(AppointmentTestFixture.reception(), no));
        assertEquals(SlotStatus.BOOKED, fixture.slots.findById("s1").orElseThrow().getStatus(),
                "the slot must not be released while the bill stands");
    }

    @Test
    void aCompletedAppointmentCannotBeCancelled() {
        // Once the treatment has been recorded, the visit is a financial record in
        // progress, and the Cancel button disappears with the transaction: cancelling a
        // completed appointment is the same mistake as cancelling a billed one.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Cleaned and polished");

        assertThrows(IllegalStateException.class,
                () -> fixture.service.cancel(AppointmentTestFixture.reception(), no));
        assertEquals(AppointmentStatus.COMPLETED, fixture.service.require(no).getStatus());
        assertEquals(SlotStatus.BOOKED, fixture.slots.findById("s1").orElseThrow().getStatus(),
                "the slot must not be released");
    }

    @Test
    void aCancelledAppointmentCannotBeCompleted() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.cancel(nimal, no);

        assertThrows(IllegalStateException.class,
                () -> fixture.service.complete(AppointmentTestFixture.silva(), no, "Anything"));
    }

    @Test
    void anAppointmentCannotBeCompletedTwice() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "First");

        assertThrows(IllegalStateException.class,
                () -> fixture.service.complete(AppointmentTestFixture.silva(), no, "Second"));
        assertEquals("First", fixture.service.require(no).getDiagnosis());
    }

    @Test
    void cancellingReleasesTheSlotAndClearsItsNumber() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        fixture.service.cancel(nimal, no);

        var slot = fixture.slots.findById("s1").orElseThrow();
        assertEquals(SlotStatus.OPEN, slot.getStatus());
        // appointment_no carries a unique key, so a released slot keeping its old number
        // would block the next booking of that time.
        assertNull(slot.getAppointmentNo());
    }

    @Test
    void aReleasedSlotCanBeBookedAgain() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.cancel(nimal, no);

        assertNotNull(fixture.service.book(kamala, "s1", "t-checkup", null).appointmentNo());
    }

    // --- completing: ownership, not just role -------------------------

    @Test
    void onlyTheTreatingDentistMayCompleteAnAppointment() {
        // The defect: AccessControl.require(user, Role.DENTIST) let any dentist write a
        // diagnosis into any other dentist's appointment.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.complete(AppointmentTestFixture.jayasuriya(), no, "Not mine"));

        assertEquals(AppointmentStatus.CONFIRMED, fixture.service.require(no).getStatus());
        assertNull(fixture.service.require(no).getDiagnosis());
    }

    @Test
    void theTreatingDentistMayComplete() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        AppointmentDetailResponse done =
                fixture.service.complete(AppointmentTestFixture.silva(), no, "Scaling, no decay");

        assertEquals(AppointmentStatus.COMPLETED, done.status());
        assertEquals("Scaling, no decay", done.diagnosis());
    }

    @Test
    void neitherReceptionNorTheAdministratorMayComplete() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.complete(AppointmentTestFixture.reception(), no, "x"));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.complete(AppointmentTestFixture.admin(), no, "x"));
    }

    @Test
    void futureAppointmentCannotBeCompleted() {
        // GAP-DEN-06: patients may book into the future, but treatment cannot be
        // recorded before the visit. A completed-but-unborn appointment would be
        // billable by reception before the patient has sat down.
        fixture.addSlot("sfuture", "d-silva", LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        String no = fixture.service.book(nimal, "sfuture", "t-checkup", null).appointmentNo();

        assertThrows(IllegalStateException.class,
                () -> fixture.service.complete(AppointmentTestFixture.silva(), no, "Too soon"));

        assertEquals(AppointmentStatus.CONFIRMED, fixture.service.require(no).getStatus());
        assertNull(fixture.service.require(no).getDiagnosis());
    }

    // --- cancelling: whose appointment --------------------------------

    @Test
    void aPatientMayNotCancelSomeoneElsesAppointment() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.cancel(kamala, no));
        assertEquals(AppointmentStatus.CONFIRMED, fixture.service.require(no).getStatus());
    }

    @Test
    void receptionMayCancelAnyAppointment() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertEquals(AppointmentStatus.CANCELLED,
                fixture.service.cancel(AppointmentTestFixture.reception(), no).status());
    }

    // --- confidentiality ----------------------------------------------

    @Test
    void theTreatingDentistAndThePatientSeeTheDiagnosis() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Scaling, no decay");

        assertInstanceOf(AppointmentDetailResponse.class,
                fixture.service.findDetail(AppointmentTestFixture.silva(), no));
        assertInstanceOf(AppointmentDetailResponse.class, fixture.service.findDetail(nimal, no));
    }

    @Test
    void receptionAndTheAdministratorDoNot() {
        // Not a stripped field - a different type, with no diagnosis component at all.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Scaling, no decay");

        assertInstanceOf(AppointmentResponse.class,
                fixture.service.findDetail(AppointmentTestFixture.reception(), no));
        assertInstanceOf(AppointmentResponse.class,
                fixture.service.findDetail(AppointmentTestFixture.admin(), no));
    }

    @Test
    void anotherDentistCannotReadTheAppointmentAtAll() {
        // Stricter than "sees it without the diagnosis". Holding the DENTIST role is not
        // the same as treating this patient, and a colleague's appointment is not a
        // dentist's business - so it is refused rather than downgraded.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Scaling, no decay");

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.findDetail(AppointmentTestFixture.jayasuriya(), no));
    }

    @Test
    void aPatientCannotReadAnotherPatientsAppointment() {
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.findDetail(kamala, no));
    }

    // --- the list queries that did not exist --------------------------

    @Test
    void aDayCanBeListed() {
        fixture.service.book(nimal, "s1", "t-checkup", null);
        fixture.service.book(kamala, "s2", "t-checkup", null);

        List<AppointmentResponse> day = fixture.service
                .onDate(AppointmentTestFixture.reception(), AppointmentTestFixture.DAY);

        assertEquals(2, day.size());
        assertEquals(List.of(LocalTime.of(16, 0), LocalTime.of(16, 30)),
                day.stream().map(AppointmentResponse::time).toList(), "sorted by time");
    }

    @Test
    void aPatientMayNotListTheDay() {
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.service.onDate(nimal, AppointmentTestFixture.DAY));
    }

    @Test
    void aPatientSeesOnlyTheirOwn() {
        fixture.service.book(nimal, "s1", "t-checkup", null);
        fixture.service.book(kamala, "s2", "t-checkup", null);

        assertEquals(List.of("p-nimal"),
                fixture.service.forSelf(nimal).stream().map(AppointmentResponse::patientId).toList());
    }

    @Test
    void aPatientSeesTheirOwnDiagnosisButNotAnothers() {
        // GAP-DEN-09: the patient dashboard must show the dentist's comment on their
        // own finished visits, and nothing for a visit belonging to someone else.
        String mine = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        String other = fixture.service.book(kamala, "s2", "t-checkup", null).appointmentNo();
        fixture.service.complete(AppointmentTestFixture.silva(), mine, "Scaling, no decay");
        fixture.service.complete(AppointmentTestFixture.silva(), other, "Root canal done");

        List<AppointmentDetailResponse> mineDetails = fixture.service.forSelfDetail(nimal);

        assertEquals(List.of("Scaling, no decay"),
                mineDetails.stream().map(AppointmentDetailResponse::diagnosis).toList());
        assertTrue(mineDetails.stream().noneMatch(d -> d.diagnosis().contains("Root canal")));
    }

    @Test
    void aDentistSeesOnlyTheirOwnDay() {
        fixture.addSlot("s3", "d-jaya", AppointmentTestFixture.DAY, LocalTime.of(17, 0));
        fixture.service.book(nimal, "s1", "t-checkup", null);
        fixture.service.book(kamala, "s3", "t-checkup", null);

        assertEquals(1, fixture.service
                .forDentistOn(AppointmentTestFixture.silva(), AppointmentTestFixture.DAY).size());
        assertEquals(1, fixture.service
                .forDentistOn(AppointmentTestFixture.jayasuriya(), AppointmentTestFixture.DAY).size());
    }

    @Test
    void aDentistWithNoRecordGetsAClearFailure() {
        ClinicPrincipal unlinked = new ClinicPrincipal("u-nobody", "Nobody",
                com.sunrise.clinic.access.domain.Role.DENTIST);

        assertThrows(ResourceNotFoundException.class,
                () -> fixture.service.forDentistOn(unlinked, AppointmentTestFixture.DAY));
    }

    // --- the audit trail ----------------------------------------------

    @Test
    void theEventRecordsWhoActedRatherThanWhoBooked() {
        // AuditObserver fell back to appointment.createdByUid, so an administrator
        // cancelling a patient's appointment appeared in the trail as the patient.
        List<AppointmentEvent> seen = new ArrayList<>();
        AppointmentTestFixture audited = new AppointmentTestFixture(List.of(seen::add));
        audited.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        audited.addSlot("s1", LocalTime.of(16, 0));
        ClinicPrincipal patient = new ClinicPrincipal("u-pat1", "Nimal Perera",
                com.sunrise.clinic.access.domain.Role.PATIENT);

        String no = audited.service.book(patient, "s1", "t-checkup", null).appointmentNo();
        audited.service.cancel(AppointmentTestFixture.admin(), no);

        assertEquals(2, seen.size());
        assertEquals("u-pat1", seen.get(0).actorUid());
        assertEquals("u-admin", seen.get(1).actorUid(), "the canceller, not the booker");
        assertEquals("ADMIN", seen.get(1).actorRole());
    }

    @Test
    void aFailingObserverDoesNotFailTheBooking() {
        // The appointment is committed by the time observers run. Throwing here would
        // report a failure for a booking that happened, and the patient would book twice.
        AppointmentObserver broken = event -> {
            throw new IllegalStateException("the mail server is down");
        };
        AppointmentTestFixture withBroken = new AppointmentTestFixture(List.of(broken));
        withBroken.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        withBroken.addSlot("s1", LocalTime.of(16, 0));
        ClinicPrincipal patient = new ClinicPrincipal("u-pat1", "Nimal Perera",
                com.sunrise.clinic.access.domain.Role.PATIENT);

        assertNotNull(withBroken.service.book(patient, "s1", "t-checkup", null));
        assertEquals(1, withBroken.appointments.count());
    }

    @Test
    void aCancellableFlagMatchesTheStatusMachine() {
        // The screens use this to decide whether to offer a Cancel button, so it must
        // agree with what the service would actually allow.
        String no = fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
        assertTrue(fixture.service.findByNo(nimal, no).isCancellable());

        fixture.service.cancel(nimal, no);
        assertFalse(fixture.service.findByNo(nimal, no).isCancellable());
    }
}
