package com.sunrise.clinic.scheduling;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.data.InMemoryDentistRepository;
import com.sunrise.clinic.scheduling.data.InMemorySessionRepository;
import com.sunrise.clinic.scheduling.data.InMemorySlotRepository;
import com.sunrise.clinic.scheduling.data.InMemoryTreatmentRepository;
import com.sunrise.clinic.scheduling.data.SessionRepository;
import com.sunrise.clinic.scheduling.data.SlotRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.Slot;
import com.sunrise.clinic.scheduling.domain.SlotResponse;
import com.sunrise.clinic.scheduling.domain.SlotStatus;
import com.sunrise.clinic.scheduling.service.ReferenceService;
import com.sunrise.clinic.scheduling.service.SlotService;
import com.sunrise.clinic.scheduling.service.SlotService.NewSession;
import com.sunrise.clinic.scheduling.service.SlotService.Published;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} in step 3b and substantially extended, because the
 * service gained the four validation rules it was missing.
 *
 * <p>Dates are relative to today rather than fixed. A test that published on
 * 2026-09-02 would start failing the day that date passed, once the service began
 * refusing dates in the past.</p>
 */
class SlotServiceTest {

    private static final ClinicPrincipal RECEPTION =
            new ClinicPrincipal("u-recep", "Kumari Silva", Role.RECEPTIONIST);
    private static final ClinicPrincipal PATIENT =
            new ClinicPrincipal("u-pat1", "Nimal Perera", Role.PATIENT);
    private static final ClinicPrincipal DENTIST_USER =
            new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    private SessionRepository sessions;
    private SlotRepository slots;
    private SlotService service;

    @BeforeEach
    void setUp() {
        InMemoryDentistRepository dentists = new InMemoryDentistRepository();
        dentists.save(Dentist.builder().id("d-silva").userUid("u-dent1")
                .name("Ranil Silva").specialization("General Dentistry")
                .consultationFee(new BigDecimal("1500.00")).active(true).build());
        dentists.save(Dentist.builder().id("d-jaya").name("Malini Jayasuriya")
                .specialization("Orthodontics")
                .consultationFee(new BigDecimal("2500.00")).active(true).build());

        sessions = new InMemorySessionRepository();
        slots = new InMemorySlotRepository();
        service = new SlotService(sessions, slots,
                new ReferenceService(dentists, new InMemoryTreatmentRepository()));
    }

    // --- publishing, the happy path -----------------------------------

    @Test
    void publishingAWindowGeneratesItsSlots() {
        Published published = service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));

        assertEquals(6, published.slots().size(), "09:00-12:00 in 30-minute slots is six");
        assertEquals(List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30"),
                published.slots().stream().map(s -> s.startTime().toString()).toList());
        assertFalse(published.hasWarning());
    }

    @Test
    void everyGeneratedSlotStartsOpenAndCarriesTheDentistsName() {
        SlotResponse first = service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("10:00"), 30))
                .slots().get(0);

        assertEquals(SlotStatus.OPEN, first.status());
        // layered's SlotResponse carried only dentistId, so a screen listing a week
        // across every dentist could not say whose slot each row was.
        assertEquals("Ranil Silva", first.dentistName());
    }

    @Test
    void republishingTheIdenticalWindowIsIdempotent() {
        // The slot id is dentistId_date_startTime, so the same window produces the
        // same ids. This is the property that makes an overlap dangerous, and it is
        // also what stops a double submission creating duplicate slots.
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("10:00"), 30));
        long after = slots.count();

        sessions.deleteById(sessions.findAll().get(0).getId());
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("10:00"), 30));

        assertEquals(after, slots.count(), "the same window must not add more slots");
    }

    // --- fix 1: an unknown dentist ------------------------------------

    @Test
    void anUnknownDentistIsNotFoundRatherThanAServerError() {
        // The id reached the insert, violated fk_session_dentist, and surfaced as a
        // 500 carrying a SQL message.
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
                () -> service.publishSession(RECEPTION,
                        new NewSession("d-nobody", TOMORROW, at("09:00"), at("12:00"), 30)));

        assertTrue(thrown.getMessage().contains("d-nobody"), thrown.getMessage());
        assertEquals(0, sessions.count(), "nothing may be written when the dentist is unknown");
    }

    // --- fix 2: an overlapping window ---------------------------------

    @Test
    void anOverlappingWindowIsRefused() {
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));

        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("11:00"), at("13:00"), 30)));
    }

    @Test
    void anOverlappingWindowCannotUnbookAnExistingAppointment() {
        // The damage the refusal prevents. Slot ids are deterministic, so the second
        // window's 11:00 slot would overwrite the first's - resetting a booked slot to
        // OPEN and quietly detaching a patient's appointment from it.
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));
        Slot booked = slots.findAll().stream()
                .filter(s -> s.getStartTime().equals(at("11:00"))).findFirst().orElseThrow();
        booked.setStatus(SlotStatus.BOOKED);
        booked.setAppointmentNo("APT-0001");
        slots.save(booked);

        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("11:00"), at("13:00"), 30)));

        Slot after = slots.findById(booked.getId()).orElseThrow();
        assertEquals(SlotStatus.BOOKED, after.getStatus());
        assertEquals("APT-0001", after.getAppointmentNo());
    }

    @Test
    void anAdjacentWindowIsAllowed() {
        // Half-open intervals: 09:00-12:00 then 12:00-15:00 is how a full day is
        // published, and treating that as an overlap would make it impossible.
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("12:00"), at("15:00"), 30));

        assertEquals(2, sessions.count());
        assertEquals(12, slots.count());
    }

    @Test
    void anOverlapForADifferentDentistIsAllowed() {
        // Two dentists work the same hours in different chairs.
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));
        service.publishSession(RECEPTION, new NewSession("d-jaya", TOMORROW, at("09:00"), at("12:00"), 30));

        assertEquals(2, sessions.count());
    }

    @Test
    void anOverlapOnADifferentDateIsAllowed() {
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30));
        service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW.plusDays(1), at("09:00"), at("12:00"), 30));

        assertEquals(2, sessions.count());
    }

    // --- fix 3: a date in the past ------------------------------------

    @Test
    void aDateInThePastIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", LocalDate.now().minusDays(1), at("09:00"), at("12:00"), 30)));
    }

    @Test
    void todayIsAllowed() {
        // Refusing today would stop the desk publishing the afternoon this morning.
        assertEquals(6, service.publishSession(RECEPTION,
                new NewSession("d-silva", LocalDate.now(), at("09:00"), at("12:00"), 30))
                .slots().size());
    }

    // --- fix 4: an uneven window --------------------------------------

    @Test
    void anUnevenWindowWarnsAboutTheRemainder() {
        // 80 minutes in 30-minute slots is two slots and twenty minutes discarded.
        Published published = service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("16:00"), at("17:20"), 30));

        assertEquals(2, published.slots().size());
        assertTrue(published.hasWarning());
        assertTrue(published.warning().contains("20 minutes"), published.warning());
    }

    @Test
    void anEvenWindowWarnsAboutNothing() {
        assertNull(service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30)).warning());
    }

    @Test
    void aSlotLongerThanTheWindowIsRefused() {
        // Previously this published a session and generated no slots at all - an
        // availability window with nothing bookable in it.
        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("09:20"), 30)));
        assertEquals(0, sessions.count());
    }

    // --- the rest of the validation -----------------------------------

    @Test
    void endBeforeOrEqualToStartIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("12:00"), at("09:00"), 30)));
        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("09:00"), 30)));
    }

    @Test
    void aNonPositiveSlotLengthIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.publishSession(RECEPTION,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 0)));
    }

    // --- who may publish ----------------------------------------------

    @Test
    void onlyStaffWhoHoldPublishAvailabilityMayPublish() {
        assertThrows(AccessControl.AccessDeniedException.class, () -> service.publishSession(PATIENT,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30)));
        // A dentist does not publish their own hours in this clinic - reception does.
        assertThrows(AccessControl.AccessDeniedException.class, () -> service.publishSession(DENTIST_USER,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30)));
    }

    @Test
    void theCheckHappensBeforeAnythingIsWritten() {
        assertThrows(AccessControl.AccessDeniedException.class, () -> service.publishSession(PATIENT,
                new NewSession("d-silva", TOMORROW, at("09:00"), at("12:00"), 30)));

        assertEquals(0, sessions.count());
        assertEquals(0, slots.count());
    }

    // --- browsing -----------------------------------------------------

    @Test
    void openSlotsExcludeBookedOnesButAllSlotsDoNot() {
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("11:00"), 30));
        Slot first = slots.findAll().stream()
                .min(java.util.Comparator.comparing(Slot::getStartTime)).orElseThrow();
        first.setStatus(SlotStatus.BOOKED);
        slots.save(first);

        assertEquals(3, service.openSlots("d-silva", TOMORROW).size());
        assertEquals(4, service.allSlots("d-silva", TOMORROW).size(),
                "the publishing screen must show what is already taken");
    }

    @Test
    void openSlotsAreSortedByTime() {
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("11:00"), 30));

        List<LocalTime> times = service.openSlots("d-silva", TOMORROW).stream()
                .map(SlotResponse::startTime).toList();

        assertEquals(times.stream().sorted().toList(), times);
    }

    @Test
    void aWeekSpansDatesAndNamesEveryDentist() {
        service.publishSession(RECEPTION, new NewSession("d-silva", TOMORROW, at("09:00"), at("10:00"), 30));
        service.publishSession(RECEPTION,
                new NewSession("d-jaya", TOMORROW.plusDays(1), at("09:00"), at("10:00"), 30));

        List<SlotResponse> week = service.openSlotsBetween(TOMORROW, TOMORROW.plusDays(6));

        assertEquals(4, week.size());
        assertEquals(List.of("Ranil Silva", "Ranil Silva", "Malini Jayasuriya", "Malini Jayasuriya"),
                week.stream().map(SlotResponse::dentistName).toList());
    }

    @Test
    void aBackwardsRangeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> service.openSlotsBetween(TOMORROW, TOMORROW.minusDays(2)));
    }

    @Test
    void browsingAnUnknownDentistIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.openSlots("d-nobody", TOMORROW));
    }

    private static LocalTime at(String time) {
        return LocalTime.parse(time);
    }
}
