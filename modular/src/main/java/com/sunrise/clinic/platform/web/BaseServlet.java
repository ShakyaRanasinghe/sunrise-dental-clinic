package com.sunrise.clinic.platform.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.access.web.AuthenticationFilter;
import com.sunrise.clinic.platform.di.AppContext;
import com.sunrise.clinic.platform.di.ClinicServletContext;
import com.sunrise.clinic.platform.error.DataAccessException;
import com.sunrise.clinic.platform.error.ErrorResponse;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.error.SlotUnavailableException;
import com.sunrise.clinic.platform.json.Json;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviour for the API servlets.
 *
 * <p>Three things every JSON endpoint needs, in one place:</p>
 *
 * <ul>
 *   <li><b>Access to the application</b> — {@link #app()} returns the shared
 *       {@link AppContext} built at start-up.</li>
 *   <li><b>Reading and writing JSON</b> — {@link #readBody} and {@link #writeJson},
 *       so no servlet repeats the content-type and encoding handling.</li>
 *   <li><b>Turning exceptions into responses</b> — {@link #handle} maps each
 *       exception the domain can throw onto its HTTP status and a safe error body.
 *       This is the replacement for the framework's exception-handler advice, and
 *       it keeps the same contract the front-end already expects: a stable
 *       {@code errorCode} plus a human-readable message, never a stack trace.</li>
 * </ul>
 *
 * <p>Subclasses put their work inside {@link #handle}, so a bug in one endpoint
 * produces a clean 500 with a logged cause rather than a container error page.</p>
 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(BaseServlet.class.getName());

    /** Work that produces a response and may fail. */
    @FunctionalInterface
    protected interface Endpoint {
        void run() throws IOException;
    }

    protected AppContext app() {
        return ClinicServletContext.get(getServletContext());
    }

    /** @return the signed-in user, or null for an anonymous request. */
    protected ClinicPrincipal currentUser(HttpServletRequest request) {
        return AuthenticationFilter.currentPrincipal(request);
    }

    // ------------------------------------------------------------------
    // Request / response helpers
    // ------------------------------------------------------------------

    /** Read and parse a JSON request body. */
    protected Map<String, Object> readBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return Json.parseObject(body.toString());
    }

    /** Write a value as a JSON response with the given status. */
    protected void writeJson(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(Json.write(value));
    }

    /** Write a 200 response. */
    protected void writeJson(HttpServletResponse response, Object value) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, value);
    }

    /** A required query parameter. */
    protected String requiredParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /** A required ISO date query parameter ({@code 2026-07-20}). */
    protected LocalDate requiredDate(HttpServletRequest request, String name) {
        String raw = requiredParam(request, name);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " must be a date in yyyy-MM-dd format");
        }
    }

    /**
     * The trailing path of the request, split on '/'.
     *
     * <p>Servlet mappings are prefixes, not templates, so the parts of the URL that
     * a framework would have bound to {@code @PathVariable} are extracted here
     * instead. For {@code /api/appointments/APT-1/cancel} mapped at
     * {@code /api/appointments/*} this returns {@code ["APT-1", "cancel"]}.</p>
     */
    protected List<String> pathParts(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return List.of();
        }
        return List.of(path.substring(1).split("/"));
    }

    // ------------------------------------------------------------------
    // Exception handling
    // ------------------------------------------------------------------

    /**
     * Run an endpoint, converting any exception into the right HTTP response.
     *
     * <p>Client mistakes (bad input, missing record, a slot someone else just took)
     * are reported as themselves. Anything unexpected is logged in full and reported
     * as a generic 500, so internal details never reach the caller.</p>
     */
    protected void handle(HttpServletResponse response, Endpoint endpoint) throws IOException {
        try {
            endpoint.run();
        } catch (ResourceNotFoundException e) {
            error(response, HttpServletResponse.SC_NOT_FOUND, "not_found", e.getMessage());
        } catch (SlotUnavailableException e) {
            // 409 Conflict — the double-booking guard reporting that it worked.
            error(response, HttpServletResponse.SC_CONFLICT, "slot_unavailable", e.getMessage());
        } catch (AccessControl.NotAuthenticatedException e) {
            // Must precede AccessDeniedException: it is a subclass, so the wider
            // catch would swallow it and answer 403 where 401 is correct.
            error(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthenticated", e.getMessage());
        } catch (AccessControl.AccessDeniedException e) {
            error(response, HttpServletResponse.SC_FORBIDDEN, "forbidden", e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, HttpServletResponse.SC_BAD_REQUEST, "bad_request", e.getMessage());
        } catch (DataAccessException e) {
            log.log(Level.SEVERE, "database_error", e);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal_error", "Something went wrong. Please try again.");
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "unhandled_exception", e);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal_error", "Something went wrong. Please try again.");
        }
    }

    private void error(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            // Part of a response is already on the wire; nothing useful can be added.
            log.warning("error_after_response_committed code=" + code);
            return;
        }
        response.reset();
        writeJson(response, status, new ErrorResponse(code, message));
    }
}
