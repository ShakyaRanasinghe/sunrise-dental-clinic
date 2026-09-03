package com.sunrise.clinic.appointments;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.appointments.data.InMemoryAppointmentRepository;
import com.sunrise.clinic.appointments.data.InMemoryCounterRepository;
import com.sunrise.clinic.appointments.service.AppointmentEventPublisher;
import com.sunrise.clinic.appointments.service.AppointmentNumberGenerator;
import com.sunrise.clinic.appointments.service.AppointmentObserver;
import com.sunrise.clinic.appointments.service.AppointmentService;
import com.sunrise.clinic.appointments.service.ClinicAccess;
import com.sunrise.clinic.patients.data.InMemoryPatientNoteRepository;
import com.sunrise.clinic.patients.data.InMemoryPatientRepository;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.service.PatientNoteService;
import com.sunrise.clinic.platform.data.SerialTransactionRunner;
import com.sunrise.clinic.scheduling.data.InMemoryDentistRepository;
import com.sunrise.clinic.scheduling.data.InMemorySlotRepository;
import com.sunrise.clinic.scheduling.data.InMemoryTreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.Slot;
import com.sunrise.clinic.scheduling.domain.SlotStatus;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The object graph {@link AppointmentServiceTest} and {@link BookingConcurrencyTest} both
 * need, assembled once.
 *
 * <p>Booking touches four modules - it reads a slot, resolves a patient, names a dentist
 * and a treatment - so wiring it takes seven collaborators. Duplicating that in two tests
 * would mean two graphs that could drift apart, and the concurrency test would stop
 * exercising the same code as the behavioural one.</p>
 *
 * <p>Everything is in-memory. {@link SerialTransactionRunner} provides the exclusion that
 * {@code SELECT … FOR UPDATE} provides in the running application, so the service logic
 * under test is identical either way - which is the point of depending on the repository
 * interface rather than on JDBC.</p>
 */
public class AppointmentTestFixture {

    public static final LocalDate DAY = LocalDate.of(2026, 7, 20);

    public final InMemorySlotRepository slots = new InMemorySlotRepository();
    public final InMemoryAppointmentRepository appointments = new InMemoryAppointmentRepository();
    public final InMemoryPatientRepository patients = new InMemoryPatientRepository();
    public final InMemoryDentistRepository dentists = new InMemoryDentistRepository();
    public final InMemoryTreatmentRepository treatments = new InMemoryTreatmentRepository();
    public final InMemoryCounterRepository counters = new InMemoryCounterRepository();
    public final InMemoryPatientNoteRepository patientNotes = new InMemoryPatientNoteRepository();

    public final ClinicAccess clinicAccess;
    public final PatientNoteService noteService;
    public final AppointmentService service;

    public AppointmentTestFixture() {
        this(List.of());
    }

    public AppointmentTestFixture(List<AppointmentObserver> observers) {
        dentists.save(Dentist.builder().id("d-silva").userUid("u-dent1")
                .name("Dr. Ranil Silva").specialization("General Dentistry")
                .consultationFee(new BigDecimal("1500.00")).active(true).build());
        dentists.save(Dentist.builder().id("d-jaya").userUid("u-dent2")
                .name("Dr. Malini Jayasuriya").specialization("Orthodontics")
                .consultationFee(new BigDecimal("2500.00")).active(true).build());
        treatments.save(Treatment.builder().id("t-checkup").name("Routine check-up")
                .baseCost(new BigDecimal("1000.00")).active(true).build());

        ReferenceService reference = new ReferenceService(dentists, treatments,
                new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository());
        clinicAccess = new ClinicAccess(patients, dentists);
        // The real adapter, so a dentist in these tests can read the notes of a patient
        // they actually have an appointment with and nobody else's.
        noteService = new PatientNoteService(patientNotes, patients,
                new com.sunrise.clinic.appointments.service.AppointmentTreatmentRelationship(
                        appointments, dentists));
        service = new AppointmentService(slots, appointments, reference, clinicAccess,
                new AppointmentNumberGenerator(counters), noteService,
                new AppointmentEventPublisher(observers),
                new SerialTransactionRunner());
    }

    /** A patient with a portal account. */
    public ClinicPrincipal addPatient(String id, String uid, String name) {
        patients.save(Patient.builder().id(id).userUid(uid).name(name)
                .email(id + "@example.lk").contactNumber("0770000000").build());
        return new ClinicPrincipal(uid, name, Role.PATIENT);
    }

    /** An open slot with Dr. Silva. */
    public Slot addSlot(String id, LocalTime start) {
        return addSlot(id, "d-silva", DAY, start);
    }

    public Slot addSlot(String id, String dentistId, LocalDate date, LocalTime start) {
        Slot slot = Slot.builder().id(id).sessionId("sess-1").dentistId(dentistId)
                .date(date).startTime(start).durationMinutes(30)
                .status(SlotStatus.OPEN).build();
        slots.save(slot);
        return slot;
    }

    public static ClinicPrincipal reception() {
        return new ClinicPrincipal("u-recep", "Kumari Silva", Role.RECEPTIONIST);
    }

    public static ClinicPrincipal admin() {
        return new ClinicPrincipal("u-admin", "Anoma Fernando", Role.ADMIN);
    }

    /** The dentist behind d-silva. */
    public static ClinicPrincipal silva() {
        return new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);
    }

    /** A different dentist, for the "not yours to treat" case. */
    public static ClinicPrincipal jayasuriya() {
        return new ClinicPrincipal("u-dent2", "Dr. Malini Jayasuriya", Role.DENTIST);
    }
}
