package com.sunrise.clinic.platform.di;

import com.sunrise.clinic.access.data.UserDao;
import com.sunrise.clinic.access.data.UserRepository;
import com.sunrise.clinic.access.service.AuthService;
import com.sunrise.clinic.access.service.LoginAttemptService;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.platform.audit.AuditDao;
import com.sunrise.clinic.platform.audit.AuditRepository;
import com.sunrise.clinic.platform.config.AppConfig;
import com.sunrise.clinic.platform.data.JdbcTransactionRunner;
import com.sunrise.clinic.platform.data.TransactionRunner;
import com.sunrise.clinic.appointments.service.AppointmentTreatmentRelationship;
import com.sunrise.clinic.notifications.data.NotificationDao;
import com.sunrise.clinic.notifications.service.EmailChannel;
import com.sunrise.clinic.notifications.service.NotificationChannelFactory;
import com.sunrise.clinic.notifications.service.NotificationObserver;
import com.sunrise.clinic.notifications.service.SmsChannel;
import com.sunrise.clinic.notifications.data.NotificationRepository;
import com.sunrise.clinic.patients.data.PatientDao;
import com.sunrise.clinic.patients.data.PatientNoteDao;
import com.sunrise.clinic.patients.data.PatientNoteRepository;
import com.sunrise.clinic.patients.service.PatientNoteService;
import com.sunrise.clinic.patients.service.SelfRegistrationService;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.appointments.data.AppointmentDao;
import com.sunrise.clinic.appointments.data.AppointmentRepository;
import com.sunrise.clinic.appointments.data.CounterDao;
import com.sunrise.clinic.appointments.data.CounterRepository;
import com.sunrise.clinic.appointments.service.AppointmentEventPublisher;
import com.sunrise.clinic.appointments.service.AppointmentNumberGenerator;
import com.sunrise.clinic.appointments.service.AppointmentService;
import com.sunrise.clinic.appointments.service.AuditObserver;
import com.sunrise.clinic.appointments.service.ClinicAccess;
import com.sunrise.clinic.billing.data.BillDao;
import com.sunrise.clinic.billing.data.BillRepository;
import com.sunrise.clinic.billing.service.BillingService;
import com.sunrise.clinic.billing.service.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import com.sunrise.clinic.feedback.data.ComplaintDao;
import com.sunrise.clinic.feedback.data.ComplaintRepository;
import com.sunrise.clinic.feedback.data.ReviewDao;
import com.sunrise.clinic.feedback.data.ReviewRepository;
import com.sunrise.clinic.feedback.service.ComplaintService;
import com.sunrise.clinic.feedback.service.ReviewService;
import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.reporting.data.JdbcReportDao;
import com.sunrise.clinic.reporting.data.ReportRepository;
import com.sunrise.clinic.reporting.service.AccountAdminService;
import com.sunrise.clinic.reporting.service.ReportService;
import com.sunrise.clinic.scheduling.data.DentistDao;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.data.SessionDao;
import com.sunrise.clinic.scheduling.data.SessionRepository;
import com.sunrise.clinic.scheduling.data.SlotDao;
import com.sunrise.clinic.scheduling.data.SlotRepository;
import com.sunrise.clinic.scheduling.data.TreatmentDao;
import com.sunrise.clinic.scheduling.data.TreatmentRepository;
import com.sunrise.clinic.scheduling.service.ReferenceService;
import com.sunrise.clinic.scheduling.service.SlotService;

/**
 * The composition root: builds the object graph once, at start-up.
 *
 * <p>Written by hand rather than delegated to a container (CON-01). It is a list
 * of constructor calls you can read top to bottom — no annotations to decode, no
 * reflection to reason about, and a stack trace that names real methods.</p>
 *
 * <h2>Accessors expose services, never repositories</h2>
 *
 * <p>This is the one deliberate difference from the previous context, and it is
 * what makes the tier boundary checkable. That version exposed
 * {@code users()}, {@code patients()}, {@code slots()} and so on, so a servlet
 * could reach the database with {@code app().patients().search(term)} — no
 * import of a repository type, and therefore invisible to any check that reads
 * import statements. Twelve of twenty servlets did exactly that.</p>
 *
 * <p>Here every repository is private. A servlet can only reach a service, so
 * NFR-MNT-02 holds by construction rather than by discipline, and this grep
 * stays empty:</p>
 *
 * <pre>
 * grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)' --include=*.java  web-packages
 * </pre>
 *
 * <h2>Still to come</h2>
 *
 * <p>Migration is in progress, so this wires {@code platform} and {@code access}
 * only. Each later step adds its own service and its own accessor:</p>
 *
 * <ul>
 *   <li>step 3 — {@code patientService()}, {@code slotService()}, {@code referenceService()}</li>
 *   <li>step 4 — {@code appointmentService()}, and the event publisher its observers attach to</li>
 *   <li>step 5 — {@code billingService()} with its two strategies</li>
 *   <li>step 6 — the notification channel factory, and the observer that uses it</li>
 *   <li>step 7 — {@code reportService()}</li>
 *   <li>step 8 — {@code complaintService()}, {@code reviewService()}</li>
 * </ul>
 */
public class AppContext implements AutoCloseable {

    private static final java.util.logging.Logger log =
            java.util.logging.Logger.getLogger(AppContext.class.getName());

    private final AppConfig config;
    private final Database database;
    private final TransactionRunner transactionRunner;

    // Repositories are private on purpose - see the note above.
    private final UserRepository users;
    private final AuditRepository auditEvents;
    private final PatientRepository patients;
    private final PatientNoteRepository patientNotes;
    private final DentistRepository dentists;
    private final TreatmentRepository treatments;
    private final SessionRepository sessions;
    private final SlotRepository slots;
    private final AppointmentRepository appointments;
    private final CounterRepository counters;
    private final BillRepository bills;
    private final ReportRepository reports;
    private final ComplaintRepository complaints;
    private final NotificationRepository notifications;
    private final NotificationChannelFactory notificationChannels;
    private final ReviewRepository reviews;

    // Services, which are what the presentation tier may reach.
    private final LoginAttemptService loginAttempts;
    private final AuthService authService;
    private final UserAccountFactory accountFactory;
    private final PatientService patientService;
    private final PatientNoteService patientNoteService;
    private final SelfRegistrationService selfRegistrationService;
    private final ReferenceService referenceService;
    private final SlotService slotService;
    private final ClinicAccess clinicAccess;
    private final AppointmentEventPublisher appointmentEvents;
    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final ComplaintService complaintService;
    private final ReviewService reviewService;
    private final AccountAdminService accountAdminService;

    public AppContext() {
        this.config = new AppConfig();
        this.database = new Database(config);
        this.transactionRunner = new JdbcTransactionRunner(database);

        this.users = new UserDao(database);
        this.auditEvents = new AuditDao(database);
        this.patients = new PatientDao(database);
        this.patientNotes = new PatientNoteDao(database);
        this.dentists = new DentistDao(database);
        this.treatments = new TreatmentDao(database);
        this.sessions = new SessionDao(database);
        this.slots = new SlotDao(database);
        this.appointments = new AppointmentDao(database);
        this.counters = new CounterDao(database);
        this.bills = new BillDao(database);
        this.reports = new JdbcReportDao(database, clinicZone());
        this.complaints = new ComplaintDao(database);
        this.notifications = new NotificationDao(database);

        // Every channel this deployment has, indexed by the factory. Adding a real SMTP or
        // SMS transport is one more entry in this list and no change anywhere else - which is
        // the whole return on the Factory Method pattern being here (FR-NOT-05).
        this.notificationChannels = new NotificationChannelFactory(java.util.List.of(
                new EmailChannel(config.get("clinic.mail.from", "no-reply@sunrisedental.lk")),
                new SmsChannel()));
        this.reviews = new ReviewDao(database);

        this.loginAttempts = new LoginAttemptService(users);
        this.authService = new AuthService(users, loginAttempts);
        this.accountFactory = new UserAccountFactory(users);
        this.patientService = new PatientService(patients);
        // The account and the patient row are written together, so a registered patient can
        // book immediately rather than signing in to an account with no profile behind it.
        this.selfRegistrationService = new SelfRegistrationService(accountFactory, patients,
                transactionRunner);
        this.referenceService = new ReferenceService(dentists, treatments);
        this.slotService = new SlotService(sessions, slots, referenceService);
        this.clinicAccess = new ClinicAccess(patients, dentists);

        // The Observer pattern's subject. Observers are registered here, at the one place
        // that knows the whole graph, so AppointmentService never learns who is listening.
        // NotificationObserver joins them when the notifications module lands.
        // Two observers, and AppointmentService knows about neither. Both run after the
        // booking transaction has committed, and the publisher isolates each of them - so a
        // mail transport being down cannot fail a booking that has already happened.
        this.appointmentEvents = new AppointmentEventPublisher(java.util.List.of(
                new AuditObserver(auditEvents),
                new NotificationObserver(notificationChannels, notifications)));

        // The inversion described in TreatmentRelationship: patients declares the question,
        // appointments answers it, and this is the one place that knows both.
        this.patientNoteService = new PatientNoteService(patientNotes, patients,
                new AppointmentTreatmentRelationship(appointments, dentists));

        this.appointmentService = new AppointmentService(slots, appointments, referenceService,
                clinicAccess, new AppointmentNumberGenerator(counters), patientNoteService,
                appointmentEvents, transactionRunner);

        // The two strategies are chosen here, once. Swapping in a promotional pricing rule
        // or a different commission policy is an edit to this line and nothing else.
        this.billingService = new BillingService(bills, appointmentService, referenceService,
                clinicAccess, new StandardBillingStrategy(),
                new DefaultRevenueSplitStrategy(
                        config.getDecimal("clinic.revenue.dentist-treatment-share", "0.60"),
                        config.getDecimal("clinic.revenue.receptionist-service-share", "0")),
                config.getDecimal("clinic.billing.service-charge", "200"),
                transactionRunner);

        // The clinic's zone, not the server's. A clock injected rather than
        // LocalDate.now() inside the service, so a test can fix "today" and the no-show
        // derivation is checkable.
        this.reportService = new ReportService(reports, java.time.Clock.system(clinicZone()));
        this.accountAdminService = new AccountAdminService(users, accountFactory, authService,
                dentists, auditEvents);
        this.complaintService = new ComplaintService(complaints, appointmentService, clinicAccess,
                referenceService, auditEvents);
        // The clinic's zone again: the review window is measured in the clinic's days.
        this.reviewService = new ReviewService(reviews, appointmentService, clinicAccess,
                referenceService, java.time.Clock.system(clinicZone()));
    }

    /**
     * The clinic's own timezone - the one every date is judged in.
     *
     * <p>Warns rather than fails when the JVM is running somewhere else, which it normally
     * is: the container runs UTC and the clinic is in Colombo. Everything that derives a
     * date from an instant uses this value, so the mismatch is harmless - but it is worth
     * saying out loud, because a silently wrong report is the failure this replaced.</p>
     */
    private java.time.ZoneId clinicZone() {
        String configured = config.get("clinic.timezone", "Asia/Colombo");
        java.time.ZoneId zone;
        try {
            zone = java.time.ZoneId.of(configured);
        } catch (java.time.DateTimeException e) {
            log.warning("clinic_timezone_unknown value=" + configured + " using=Asia/Colombo");
            zone = java.time.ZoneId.of("Asia/Colombo");
        }
        java.time.ZoneId jvm = java.time.ZoneId.systemDefault();
        if (!jvm.equals(zone)) {
            log.info("clinic_timezone clinic=" + zone + " jvm=" + jvm
                    + " (dates are computed in the clinic's zone, not the JVM's)");
        }
        return zone;
    }

    public AppConfig config() {
        return config;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    public AuthService authService() {
        return authService;
    }

    public LoginAttemptService loginAttemptService() {
        return loginAttempts;
    }

    /**
     * The notification channels this deployment can use.
     *
     * <p>Both record rather than send, for now. The observer that calls them arrives next; until
     * then nothing writes a notification row.</p>
     */
    public NotificationChannelFactory notificationChannels() {
        return notificationChannels;
    }

    /** Patient self-registration — the only public write endpoint. */
    public SelfRegistrationService selfRegistrationService() {
        return selfRegistrationService;
    }

    /** The patient's own declared medical notes. */
    public PatientNoteService patientNoteService() {
        return patientNoteService;
    }

    /** The patient register. */
    public PatientService patientService() {
        return patientService;
    }

    /** Dentists and the treatment catalogue. */
    public ReferenceService referenceService() {
        return referenceService;
    }

    /** Published availability and the slots it produces. */
    public SlotService slotService() {
        return slotService;
    }

    /** Booking, cancelling and completing. */
    public AppointmentService appointmentService() {
        return appointmentService;
    }

    /** Bills, pricing and the revenue split. */
    public BillingService billingService() {
        return billingService;
    }

    /** Complaints about a dentist. */
    public ComplaintService complaintService() {
        return complaintService;
    }

    /** Dentist reviews, and the aggregates built from them. */
    public ReviewService reviewService() {
        return reviewService;
    }

    /** The administrator's reports, and their CSV export. */
    public ReportService reportService() {
        return reportService;
    }

    /** Creating, unlocking and deactivating accounts. */
    public AccountAdminService accountAdminService() {
        return accountAdminService;
    }

    /**
     * The audit trail, for the administrator's screen.
     *
     * <p>The one repository exposed directly, and deliberately: the trail has no behaviour
     * to wrap. A service over it would be a method that forwards a search and adds nothing,
     * and the screen guards itself with {@code READ_AUDIT}. Reads are not audited - an
     * audit of reading the audit grows without bound.</p>
     */
    public AuditRepository auditTrail() {
        return auditEvents;
    }

    /** Who may see an appointment's clinical detail. */
    public ClinicAccess clinicAccess() {
        return clinicAccess;
    }

    public UserAccountFactory accountFactory() {
        return accountFactory;
    }

    /** The audit trail is written by services, so this stays package-visible to none. */
    AuditRepository auditEvents() {
        return auditEvents;
    }

    @Override
    public void close() {
        database.close();
    }
}
