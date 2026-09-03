package com.sunrise.clinic.feedback;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.feedback.data.InMemoryComplaintRepository;
import com.sunrise.clinic.feedback.data.InMemoryReviewRepository;
import com.sunrise.clinic.feedback.service.ComplaintService;
import com.sunrise.clinic.feedback.service.ReviewService;
import com.sunrise.clinic.platform.audit.InMemoryAuditRepository;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * The graph both feedback tests need. Complaints and reviews both hang off a completed
 * appointment, so each test needs the appointments fixture underneath.
 *
 * <p>The clock is fixed, because the review window is measured in days from the visit and a
 * test reading the system clock would pass or fail depending on when it ran.</p>
 */
public class FeedbackTestFixture {

    public final AppointmentTestFixture appointments = new AppointmentTestFixture();
    public final InMemoryComplaintRepository complaints = new InMemoryComplaintRepository();
    public final InMemoryReviewRepository reviews = new InMemoryReviewRepository();
    public final InMemoryAuditRepository audit = new InMemoryAuditRepository();

    public final ComplaintService complaintService;
    public final ReviewService reviewService;

    private final LocalDate today;

    public FeedbackTestFixture(LocalDate today) {
        this.today = today;
        ReferenceService reference = new ReferenceService(appointments.dentists,
                appointments.treatments,
                new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository());
        complaintService = new ComplaintService(complaints, appointments.service,
                appointments.clinicAccess, reference, audit);
        reviewService = new ReviewService(reviews, appointments.service,
                appointments.clinicAccess, reference,
                Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        ZoneId.systemDefault()));
    }

    /** Books and treats an appointment on {@code visitDate}, so it can be rated. */
    public String treatedVisit(ClinicPrincipal patient, String slotId, LocalDate visitDate) {
        appointments.addSlot(slotId, "d-silva", visitDate, LocalTime.of(9, 0));
        String no = appointments.service.book(patient, slotId, "t-checkup", null).appointmentNo();
        appointments.service.complete(AppointmentTestFixture.silva(), no, "done");
        return no;
    }

    /** Books but does not treat, so it cannot be rated. */
    public String untreatedVisit(ClinicPrincipal patient, String slotId, LocalDate visitDate) {
        appointments.addSlot(slotId, "d-silva", visitDate, LocalTime.of(10, 0));
        return appointments.service.book(patient, slotId, "t-checkup", null).appointmentNo();
    }

    public LocalDate today() {
        return today;
    }

    /**
     * The same reviews and appointments, seen from a different date.
     *
     * <p>Needed because the review window is measured from the visit, so testing that it
     * closes means moving the clock rather than the data. Constructing a whole second
     * fixture would give a second set of empty repositories - which is exactly the mistake
     * this method exists to stop anyone making twice.</p>
     */
    public ReviewService reviewServiceAsOf(LocalDate asOf) {
        return new ReviewService(reviews, appointments.service, appointments.clinicAccess,
                new ReferenceService(appointments.dentists, appointments.treatments,
                        new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository()),
                Clock.fixed(asOf.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        ZoneId.systemDefault()));
    }
}
