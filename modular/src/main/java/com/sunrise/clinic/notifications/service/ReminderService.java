package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.appointments.data.AppointmentRepository;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.notifications.data.NotificationRepository;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationKind;
import com.sunrise.clinic.notifications.domain.NotificationStatus;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.Patient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reminds patients about tomorrow's appointments — FR-NOT-02.
 *
 * <h2>Why a sweep, and not an event</h2>
 *
 * <p>Every other message in this system is caused by somebody doing something: a booking, a
 * cancellation. A reminder is caused by <b>time passing</b>, and nothing in the application
 * happens when time passes. So it is a sweep — "which appointments are tomorrow, and which of
 * those have not been reminded" — run by something outside the application.</p>
 *
 * <p>That something is cron hitting {@code POST /api/reminders/run}, and the alternative
 * considered was a background thread in the container. The servlet won for three reasons: it
 * adds no dependency and no thread to reason about, it can be triggered by hand with
 * {@code curl} while somebody is watching, and if it stops running that is visible in a cron
 * log rather than invisible inside a JVM. The cost is honest — the clinic has to set up the cron
 * entry, and the runbook says so.</p>
 *
 * <h2>What it will not do</h2>
 *
 * <p><b>It will not send twice.</b> It is going to be asked the same question about the same
 * appointment more than once — a cron that fires twice, an overlapping run, somebody testing it
 * — and two reminders is the fastest way to teach a patient to ignore the third.</p>
 *
 * <p><b>It will not retry a failure.</b> A reminder that failed stays failed. Retrying on a
 * timer with no backoff would hammer a broken transport once a day forever, and the row saying
 * it failed is the honest record for somebody to act on.</p>
 *
 * <p><b>It will not remind about an appointment that is not happening.</b> Only CONFIRMED:
 * cancelled is not happening, and completed or billed already has.</p>
 */
public class ReminderService {

    private static final Logger log = Logger.getLogger(ReminderService.class.getName());

    /** How far ahead to look. One day: a reminder the morning before is the useful one. */
    private static final int DAYS_AHEAD = 1;

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final NotificationChannelFactory channels;
    private final NotificationRepository notifications;
    private final Clock clock;

    public ReminderService(AppointmentRepository appointments, PatientRepository patients,
                           NotificationChannelFactory channels,
                           NotificationRepository notifications, Clock clock) {
        this.appointments = appointments;
        this.patients = patients;
        this.channels = channels;
        this.notifications = notifications;
        this.clock = clock;
    }

    /**
     * What one sweep did.
     *
     * @param date      the day being reminded about
     * @param due       appointments found for it
     * @param reminded  attempts made this run
     * @param skipped   already reminded on an earlier run
     * @param failed    attempted and not delivered — including patients with no contact number
     */
    public record Sweep(LocalDate date, int due, int reminded, int skipped, int failed) {

        public boolean didAnything() {
            return reminded > 0;
        }
    }

    /** The day this sweep is about — tomorrow, in the clinic's timezone. */
    public LocalDate remindingAbout() {
        return LocalDate.now(clock).plusDays(DAYS_AHEAD);
    }

    /**
     * Remind everybody due tomorrow who has not been reminded.
     *
     * <p>Takes no caller. It is invoked by cron rather than by a person, and the endpoint in
     * front of it does the authorising — see {@code ReminderServlet}. Putting an
     * {@code AccessControl} check here would mean inventing a principal for a scheduled job,
     * which is a fiction that then has to be maintained.</p>
     */
    public Sweep run() {
        LocalDate date = remindingAbout();
        List<Appointment> due = appointments.findByDateBetween(date, date).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .toList();

        List<String> reminded = new ArrayList<>();
        int skipped = 0;
        int failed = 0;

        for (Appointment appointment : due) {
            String no = appointment.getAppointmentNo();
            if (notifications.hasBeenAttempted(no, NotificationKind.REMINDER)) {
                skipped++;
                continue;
            }
            DispatchResult result = remind(appointment);
            if (result == null) {
                // No SMS channel in this deployment. Nothing attempted, so nothing recorded —
                // and the appointment stays eligible for when one is configured.
                continue;
            }
            reminded.add(no);
            if (result.status() == NotificationStatus.FAILED) {
                failed++;
            }
        }

        Sweep sweep = new Sweep(date, due.size(), reminded.size(), skipped, failed);
        log.log(Level.INFO,
                "reminder_sweep date={0} due={1} reminded={2} skipped={3} failed={4}",
                new Object[] { date, sweep.due(), sweep.reminded(), sweep.skipped(), sweep.failed() });
        return sweep;
    }

    /** @return what the channel said, or null if this deployment has no SMS channel. */
    private DispatchResult remind(Appointment appointment) {
        Optional<NotificationChannel> channel = channels.find(ChannelType.SMS);
        if (channel.isEmpty()) {
            return null;
        }

        Optional<Patient> patient = patients.findById(appointment.getPatientId());
        String recipient = patient.map(Patient::getContactNumber).orElse(null);
        String name = patient.map(Patient::getName).orElse("Patient");

        String subject = "Appointment reminder — " + appointment.getAppointmentNo();
        String body = "Dear " + name + ", this is a reminder of your appointment tomorrow, "
                + appointment.getDate() + " at " + appointment.getTime() + "."
                + " Please contact us if you cannot make it. — Sunrise Dental Clinic";

        // dispatch, not send: a patient with no contact number comes back FAILED with the
        // reason rather than throwing, and either way the booking is long since committed.
        DispatchResult result = channel.get().dispatch(recipient, subject, body);

        notifications.save(Notification.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(appointment.getAppointmentNo())
                .channel(ChannelType.SMS)
                .kind(NotificationKind.REMINDER)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(result.status())
                .sentAt(Instant.now())
                .build());
        return result;
    }
}
