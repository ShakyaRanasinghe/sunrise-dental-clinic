package com.sunrise.clinic.app;

import com.sunrise.clinic.config.AppConfig;
import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.dao.AuditDao;
import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.dao.DentistDao;
import com.sunrise.clinic.dao.JdbcTransactionRunner;
import com.sunrise.clinic.dao.NotificationDao;
import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.dao.SessionDao;
import com.sunrise.clinic.dao.SlotDao;
import com.sunrise.clinic.dao.TreatmentDao;
import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.mapper.ClinicMapper;
import com.sunrise.clinic.pattern.billing.BillingStrategy;
import com.sunrise.clinic.pattern.billing.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.pattern.billing.RevenueSplitStrategy;
import com.sunrise.clinic.pattern.billing.StandardBillingStrategy;
import com.sunrise.clinic.pattern.factory.EmailChannel;
import com.sunrise.clinic.pattern.factory.NotificationChannel;
import com.sunrise.clinic.pattern.factory.NotificationChannelFactory;
import com.sunrise.clinic.pattern.factory.SmsChannel;
import com.sunrise.clinic.repository.TransactionRunner;
import com.sunrise.clinic.security.AuthService;
import com.sunrise.clinic.security.ClinicAccess;
import com.sunrise.clinic.security.LoginAttemptService;
import com.sunrise.clinic.service.AppointmentService;
import com.sunrise.clinic.service.BillingService;
import com.sunrise.clinic.service.ReportService;
import com.sunrise.clinic.service.SlotService;
import com.sunrise.clinic.service.notification.AppointmentEventPublisher;
import com.sunrise.clinic.service.notification.AppointmentObserver;
import com.sunrise.clinic.service.notification.AuditObserver;
import com.sunrise.clinic.service.notification.NotificationObserver;

import java.util.List;
import java.util.logging.Logger;

/**
 * Builds the object graph for the application — the hand-written replacement for
 * the dependency-injection container.
 *
 * <p>Every collaborator is created once, in one place, and passed to whoever needs
 * it through its constructor. There is no classpath scanning, no annotation
 * processing and no reflection: the wiring is ordinary Java that can be read
 * top-to-bottom, stepped through in a debugger, and checked by the compiler. If a
 * dependency is missing the build fails, rather than the application starting and
 * failing on the first request.</p>
 *
 * <p>Construction order is significant and follows the layering: configuration,
 * then the database, then repositories, then the design-pattern collaborators,
 * then services. Nothing lower in the list is visible to anything above it.</p>
 *
 * <p>One instance is created per web application by {@link ClinicServletContext}
 * and shared by every servlet, so the objects here are effectively singletons for
 * the lifetime of the deployment. They must therefore be stateless or
 * thread-safe — which the services are, holding only their collaborators.</p>
 */
public class AppContext implements AutoCloseable {

    private static final Logger log = Logger.getLogger(AppContext.class.getName());

    private final AppConfig config;
    private final Database database;

    // --- repositories (JDBC) ---
    private final UserDao users;
    private final PatientDao patients;
    private final DentistDao dentists;
    private final TreatmentDao treatments;
    private final SessionDao sessions;
    private final SlotDao slots;
    private final AppointmentDao appointments;
    private final BillDao bills;
    private final NotificationDao notifications;
    private final AuditDao auditEvents;
    private final TransactionRunner transactionRunner;

    // --- patterns ---
    private final NotificationChannelFactory channelFactory;
    private final BillingStrategy billingStrategy;
    private final RevenueSplitStrategy revenueSplitStrategy;
    private final AppointmentEventPublisher eventPublisher;

    // --- services ---
    private final AppointmentService appointmentService;
    private final SlotService slotService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    private final ClinicAccess clinicAccess;
    private final ClinicMapper mapper;

    public AppContext() {
        this.config = new AppConfig();
        this.database = new Database(config);

        // --- data access ---
        this.users = new UserDao(database);
        this.patients = new PatientDao(database);
        this.dentists = new DentistDao(database);
        this.treatments = new TreatmentDao(database);
        this.sessions = new SessionDao(database);
        this.slots = new SlotDao(database);
        this.appointments = new AppointmentDao(database);
        this.bills = new BillDao(database);
        this.notifications = new NotificationDao(database);
        this.auditEvents = new AuditDao(database);
        this.transactionRunner = new JdbcTransactionRunner(database);

        // --- Factory Method: the channels the factory can hand out ---
        List<NotificationChannel> channels = List.of(
                new EmailChannel(config.get("clinic.mail.from", "no-reply@sunrisedental.lk")),
                new SmsChannel());
        this.channelFactory = new NotificationChannelFactory(channels);

        // --- Strategy: pricing and commission policy, both configurable ---
        this.billingStrategy = new StandardBillingStrategy();
        this.revenueSplitStrategy = new DefaultRevenueSplitStrategy(
                config.getDouble("clinic.revenue.dentist-treatment-share", 0.60));

        // --- Observer: who reacts when an appointment changes ---
        List<AppointmentObserver> observers = List.of(
                new NotificationObserver(channelFactory, notifications),
                new AuditObserver(auditEvents));
        this.eventPublisher = new AppointmentEventPublisher(observers);

        // --- services ---
        this.appointmentService = new AppointmentService(
                slots, appointments, patients, eventPublisher, transactionRunner);
        this.slotService = new SlotService(sessions, slots);
        this.billingService = new BillingService(
                appointments, bills, dentists, treatments,
                billingStrategy, revenueSplitStrategy,
                config.getDouble("clinic.billing.service-charge", 200));

        this.reportService = new ReportService(bills, appointments);

        this.loginAttemptService = new LoginAttemptService();
        this.authService = new AuthService(users, loginAttemptService);
        this.clinicAccess = new ClinicAccess(patients, dentists);
        this.mapper = new ClinicMapper();

        log.info("application_context_ready");
    }

    // ------------------------------------------------------------------
    // Accessors used by the servlets
    // ------------------------------------------------------------------

    public AppConfig config() {
        return config;
    }

    public Database database() {
        return database;
    }

    public UserDao users() {
        return users;
    }

    public PatientDao patients() {
        return patients;
    }

    public DentistDao dentists() {
        return dentists;
    }

    public TreatmentDao treatments() {
        return treatments;
    }

    public SessionDao sessions() {
        return sessions;
    }

    public SlotDao slots() {
        return slots;
    }

    public AppointmentDao appointments() {
        return appointments;
    }

    public BillDao bills() {
        return bills;
    }

    public NotificationDao notifications() {
        return notifications;
    }

    public AuditDao auditEvents() {
        return auditEvents;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    public AppointmentService appointmentService() {
        return appointmentService;
    }

    public SlotService slotService() {
        return slotService;
    }

    public BillingService billingService() {
        return billingService;
    }

    public ReportService reportService() {
        return reportService;
    }

    public AuthService authService() {
        return authService;
    }

    public LoginAttemptService loginAttemptService() {
        return loginAttemptService;
    }

    public ClinicAccess clinicAccess() {
        return clinicAccess;
    }

    public ClinicMapper mapper() {
        return mapper;
    }

    /** Releases the database connections when the application is undeployed. */
    @Override
    public void close() {
        database.close();
        log.info("application_context_closed");
    }
}
