package com.sunrise.clinic.notifications;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.notifications.data.InMemoryNotificationRepository;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationKind;
import com.sunrise.clinic.notifications.domain.NotificationStatus;
import com.sunrise.clinic.notifications.service.EmailChannel;
import com.sunrise.clinic.notifications.service.NotificationChannel;
import com.sunrise.clinic.notifications.service.NotificationChannelFactory;
import com.sunrise.clinic.notifications.service.ReminderService;
import com.sunrise.clinic.notifications.service.ReminderService.Sweep;
import com.sunrise.clinic.patients.domain.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reminder sweep — FR-NOT-02.
 *
 * <p>The clock is fixed. A reminder is defined entirely by what day it is, so a test reading the
 * system clock would be testing the calendar.</p>
 */
class ReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private AppointmentTestFixture fixture;
    private InMemoryNotificationRepository notifications;
    private ClinicPrincipal nimal;

    @BeforeEach
    void setUp() {
        fixture = new AppointmentTestFixture();
        notifications = new InMemoryNotificationRepository();
        nimal = fixture.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        fixture.patients.findById("p-nimal").ifPresent(p -> {
            p.setContactNumber("0771234567");
            fixture.patients.save(p);
        });
    }

    private ReminderService sweeper(List<NotificationChannel> channels) {
        return new ReminderService(fixture.appointments, fixture.patients,
                new NotificationChannelFactory(channels), notifications,
                Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        ZoneId.systemDefault()));
    }

    private ReminderService sweeper() {
        return sweeper(List.of(new com.sunrise.clinic.notifications.service.SmsChannel()));
    }

    private String bookOn(LocalDate date, String slotId) {
        fixture.addSlot(slotId, "d-silva", date, LocalTime.of(9, 0));
        return fixture.service.book(nimal, slotId, "t-checkup", null).appointmentNo();
    }

    // --- what gets reminded ---------------------------------------------

    @Test
    void anAppointmentTomorrowIsReminded() {
        String no = bookOn(TOMORROW, "s1");

        Sweep sweep = sweeper().run();

        assertEquals(TOMORROW, sweep.date());
        assertEquals(1, sweep.due());
        assertEquals(1, sweep.reminded());
        assertEquals(NotificationKind.REMINDER,
                notifications.findByAppointmentNo(no).get(0).getKind());
        assertEquals(ChannelType.SMS, notifications.findByAppointmentNo(no).get(0).getChannel());
    }

    @Test
    void theReminderGoesToTheContactNumberAndSaysWhen() {
        String no = bookOn(TOMORROW, "s1");

        sweeper().run();
        Notification reminder = notifications.findByAppointmentNo(no).get(0);

        assertEquals("0771234567", reminder.getRecipient());
        assertTrue(reminder.getBody().contains("tomorrow"), reminder.getBody());
        assertTrue(reminder.getBody().contains(TOMORROW.toString()), reminder.getBody());
        assertTrue(reminder.getBody().contains("Nimal Perera"));
    }

    @Test
    void todayAndTheDayAfterAreNotReminded() {
        // One day ahead. A reminder the morning before is the useful one; today's is too late
        // and next week's is forgotten by the time it matters.
        bookOn(TODAY, "s-today");
        bookOn(TODAY.plusDays(2), "s-later");

        Sweep sweep = sweeper().run();

        assertEquals(0, sweep.due());
        assertEquals(0, notifications.count());
    }

    @Test
    void aCancelledAppointmentIsNotReminded() {
        // It is not happening. Reminding somebody about it would be worse than silence.
        String no = bookOn(TOMORROW, "s1");
        fixture.service.cancel(nimal, no);

        assertEquals(0, sweeper().run().due());
    }

    @Test
    void anAlreadyTreatedAppointmentIsNotReminded() {
        String no = bookOn(TOMORROW, "s1");
        fixture.service.complete(AppointmentTestFixture.silva(), no, "done");

        assertEquals(0, sweeper().run().due());
    }

    // --- it must not send twice -----------------------------------------

    @Test
    void asecondSweepSkipsRatherThanSendingAgain() {
        // It will be asked the same question twice — a cron that fires twice, an overlapping
        // run, somebody testing it. Two reminders teaches a patient to ignore the third.
        String no = bookOn(TOMORROW, "s1");

        assertEquals(1, sweeper().run().reminded());
        Sweep second = sweeper().run();

        assertEquals(0, second.reminded());
        assertEquals(1, second.skipped());
        assertEquals(1, notifications.findByAppointmentNo(no).size());
    }

    @Test
    void aFailedReminderIsNotRetried() {
        // Retrying on a timer with no backoff would hammer a broken transport once a day
        // forever. The row saying it failed is the record for somebody to act on.
        bookOn(TOMORROW, "s1");
        ReminderService broken = sweeper(List.of(new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.SMS;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                throw new IllegalStateException("the gateway is down");
            }
        }));

        Sweep first = broken.run();
        assertEquals(1, first.reminded());
        assertEquals(1, first.failed());

        assertEquals(0, broken.run().reminded(), "a failure is not retried");
        assertEquals(1, notifications.count());
    }

    @Test
    void aConfirmationDoesNotCountAsAReminder() {
        // The reason the kind is a column rather than inferred from the channel. An
        // appointment with a confirmation on the record is still due a reminder.
        String no = bookOn(TOMORROW, "s1");
        notifications.save(Notification.builder()
                .id("existing").appointmentNo(no).channel(ChannelType.EMAIL)
                .kind(NotificationKind.CONFIRMATION).recipient("nimal@example.lk")
                .subject("Confirmed").body("Confirmed")
                .status(NotificationStatus.LOGGED).sentAt(java.time.Instant.now()).build());

        assertEquals(1, sweeper().run().reminded());
    }

    // --- when it cannot ------------------------------------------------

    @Test
    void aPatientWithNoContactNumberIsRecordedAsFailed() {
        // Registering without one is allowed, so this is a real case. The row answers "why was
        // this patient not reminded" — there was nowhere to send it.
        fixture.patients.save(Patient.builder().id("p-nophone").userUid("u-nophone")
                .name("No Phone").build());
        ClinicPrincipal patient = new ClinicPrincipal("u-nophone", "No Phone",
                com.sunrise.clinic.access.domain.Role.PATIENT);
        fixture.addSlot("s2", "d-silva", TOMORROW, LocalTime.of(10, 0));
        String no = fixture.service.book(patient, "s2", "t-checkup", null).appointmentNo();

        Sweep sweep = sweeper().run();

        assertEquals(1, sweep.failed());
        assertEquals(NotificationStatus.FAILED,
                notifications.findByAppointmentNo(no).get(0).getStatus());
    }

    @Test
    void withNoSmsChannelNothingIsAttemptedAndNothingIsRecorded() {
        // And the appointment stays eligible, so configuring a gateway tomorrow does not mean
        // everybody booked today misses their reminder.
        bookOn(TOMORROW, "s1");
        ReminderService noSms = sweeper(List.of(new EmailChannel("no-reply@sunrisedental.lk")));

        Sweep sweep = noSms.run();

        assertEquals(1, sweep.due());
        assertEquals(0, sweep.reminded());
        assertEquals(0, notifications.count());
        // Still eligible once a channel exists.
        assertEquals(1, sweeper().run().reminded());
    }

    @Test
    void withNothingDueTheSweepIsQuietAndSaysSo() {
        Sweep sweep = sweeper().run();

        assertEquals(0, sweep.due());
        assertFalse(sweep.didAnything());
    }

    @Test
    void severalAppointmentsAreEachRemindedOnce() {
        bookOn(TOMORROW, "s1");
        ClinicPrincipal other = fixture.addPatient("p-b", "u-b", "Other Patient");
        fixture.patients.findById("p-b").ifPresent(p -> {
            p.setContactNumber("0759876543");
            fixture.patients.save(p);
        });
        fixture.addSlot("s2", "d-silva", TOMORROW, LocalTime.of(11, 0));
        fixture.service.book(other, "s2", "t-checkup", null);

        Sweep sweep = sweeper().run();

        assertEquals(2, sweep.due());
        assertEquals(2, sweep.reminded());
        assertEquals(2, notifications.count());
    }

    @Test
    void remindingAboutSaysWhichDayWithoutRunning() {
        // So somebody setting up the cron entry can check it without sending anything.
        assertEquals(TOMORROW, sweeper().remindingAbout());
        assertEquals(0, notifications.count());
    }
}
