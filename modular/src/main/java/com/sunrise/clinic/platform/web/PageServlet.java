package com.sunrise.clinic.platform.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.access.web.AuthenticationFilter;
import com.sunrise.clinic.platform.di.AppContext;
import com.sunrise.clinic.platform.di.ClinicServletContext;
import com.sunrise.clinic.platform.error.DataAccessException;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.error.SlotUnavailableException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviour for the servlets that render pages.
 *
 * <p>The counterpart of {@link BaseServlet}: same responsibilities — reach the
 * application, identify the user, deal with failures — but the output is HTML, so
 * the work is forwarding to a JSP rather than writing JSON.</p>
 *
 * <p>A servlet prepares the data, puts it on the request, and forwards to a view
 * under {@code /WEB-INF/jsp}. Views live under {@code WEB-INF} deliberately: the
 * container refuses to serve anything there directly, so a page can only be
 * reached through the servlet that populates it. Requesting a JSP by its own URL
 * and getting a half-rendered page with missing data is simply not possible.</p>
 */
public abstract class PageServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(PageServlet.class.getName());

    /** Page work that may fail. */
    @FunctionalInterface
    protected interface Page {
        void run() throws IOException, ServletException;
    }

    protected AppContext app() {
        return ClinicServletContext.get(getServletContext());
    }

    /** @return the signed-in user; never null on a protected page. */
    protected ClinicPrincipal currentUser(HttpServletRequest request) {
        return AuthenticationFilter.currentPrincipal(request);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * Forward to a view.
     *
     * @param view the file name under {@code /WEB-INF/jsp}, without the extension
     */
    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/jsp/" + view + ".jsp").forward(request, response);
    }

    /** Redirect to a path within the application. */
    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /** A trimmed form field, or null when it was left blank. */
    protected String field(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** A required form field. */
    protected String requiredField(HttpServletRequest request, String name, String label) {
        String value = field(request, name);
        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    /** A date form field, defaulting to today when absent. */
    protected LocalDate dateField(HttpServletRequest request, String name, LocalDate fallback) {
        String raw = field(request, name);
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please enter a valid date.");
        }
    }

    // ------------------------------------------------------------------
    // Failure handling
    // ------------------------------------------------------------------

    /**
     * Run page logic, turning a failure into a message the visitor can act on.
     *
     * <p>Expected problems (a clash, a missing record, invalid input) are shown on
     * the error page in plain language. Anything unexpected is logged in full and
     * shown as a generic apology — a stack trace on screen would be both useless to
     * a receptionist and a disclosure risk.</p>
     */
    protected void page(HttpServletRequest request, HttpServletResponse response, Page work)
            throws IOException, ServletException {
        try {
            work.run();
        } catch (SlotUnavailableException e) {
            fail(request, response, HttpServletResponse.SC_CONFLICT,
                    "That slot has just been taken",
                    "Someone booked this time while you were choosing. Please pick another slot.");
        } catch (ResourceNotFoundException e) {
            fail(request, response, HttpServletResponse.SC_NOT_FOUND,
                    "Not found", e.getMessage());
        } catch (AccessControl.AccessDeniedException e) {
            fail(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Not permitted", e.getMessage());
        } catch (IllegalStateException e) {
            // The appointment cannot make this move - already completed, already
            // billed. A conflict with the record's state, not a fault in the form,
            // and the message says which because "please try again" would be a lie.
            fail(request, response, HttpServletResponse.SC_CONFLICT,
                    "That is no longer possible", e.getMessage());
        } catch (IllegalArgumentException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Please check the form", e.getMessage());
        } catch (DataAccessException e) {
            log.log(Level.SEVERE, "page_database_error", e);
            fail(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Something went wrong",
                    "We could not reach the clinic database. Please try again in a moment.");
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "page_unhandled_exception", e);
            fail(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Something went wrong", "Please try again, or contact the front desk.");
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response,
                      int status, String heading, String message)
            throws ServletException, IOException {
        if (response.isCommitted()) {
            log.warning("error_after_response_committed heading=" + heading);
            return;
        }
        response.setStatus(status);
        request.setAttribute("errorHeading", heading);
        request.setAttribute("errorMessage", message);
        // "shared/error", not "error". The view lives in shared/, and naming it
        // "error" forwarded to /WEB-INF/jsp/error.jsp, which does not exist - so
        // EVERY page-level error answered 404 with "That address does not exist",
        // whatever had actually gone wrong. It hid a 403 as a missing page, and it
        // hid a database failure during this step's own testing. A missing view is a
        // silent 404 from the container, which is why PageServletViewsTest now
        // asserts that every name a servlet renders resolves to a file.
        render(request, response, "shared/error");
    }
}
