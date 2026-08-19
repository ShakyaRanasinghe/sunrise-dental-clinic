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
import com.sunrise.clinic.platform.db.Database;

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

    private final AppConfig config;
    private final Database database;
    private final TransactionRunner transactionRunner;

    // Repositories are private on purpose - see the note above.
    private final UserRepository users;
    private final AuditRepository auditEvents;

    // Services, which are what the presentation tier may reach.
    private final LoginAttemptService loginAttempts;
    private final AuthService authService;
    private final UserAccountFactory accountFactory;

    public AppContext() {
        this.config = new AppConfig();
        this.database = new Database(config);
        this.transactionRunner = new JdbcTransactionRunner(database);

        this.users = new UserDao(database);
        this.auditEvents = new AuditDao(database);

        this.loginAttempts = new LoginAttemptService(users);
        this.authService = new AuthService(users, loginAttempts);
        this.accountFactory = new UserAccountFactory(users);
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
