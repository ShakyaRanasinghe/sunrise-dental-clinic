package com.sunrise.clinic.app;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ties the {@link AppContext} lifecycle to the web application's.
 *
 * <p>The container calls {@link #contextInitialized} once at deployment: the object
 * graph is built and the connection pool opened, then stored in the
 * {@link ServletContext} so every servlet can reach it. On undeploy or shutdown
 * {@link #contextDestroyed} closes the pool, which matters in development where
 * repeated redeploys would otherwise leak database connections until MySQL refused
 * new ones.</p>
 *
 * <p>Building the context here rather than lazily in a servlet means a
 * misconfiguration — a wrong database password, say — fails the deployment loudly
 * instead of surfacing as a confusing error on somebody's first request.</p>
 */
public class ClinicServletContext implements ServletContextListener {

    private static final Logger log = Logger.getLogger(ClinicServletContext.class.getName());

    /** The {@link ServletContext} attribute the {@link AppContext} is stored under. */
    public static final String ATTRIBUTE = "clinicAppContext";

    private AppContext context;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            context = new AppContext();
            event.getServletContext().setAttribute(ATTRIBUTE, context);
            log.info("clinic_application_started");
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "clinic_application_failed_to_start", e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (context != null) {
            context.close();
        }
        event.getServletContext().removeAttribute(ATTRIBUTE);
        log.info("clinic_application_stopped");
    }

    /**
     * @return the application context for this web application
     * @throws IllegalStateException if the listener did not run
     */
    public static AppContext get(ServletContext servletContext) {
        Object attribute = servletContext.getAttribute(ATTRIBUTE);
        if (attribute instanceof AppContext appContext) {
            return appContext;
        }
        throw new IllegalStateException(
                "The application context is not available — did ClinicServletContext start?");
    }
}
