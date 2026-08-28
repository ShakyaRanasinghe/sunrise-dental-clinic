package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Read-only reference data the booking screens need.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET /api/dentists}</td><td>active dentists</td></tr>
 *   <tr><td>{@code GET /api/treatments}</td><td>the treatment catalogue</td></tr>
 * </table>
 *
 * <p>One servlet on two exact mappings, because they are the same kind of request: a
 * small list any signed-in user may read. {@code getServletPath()} is safe to switch
 * on here - unlike inside a forwarded JSP - because this servlet is reached directly
 * by the request, not through a dispatch.</p>
 *
 * <p>Both routes go through {@link com.sunrise.clinic.scheduling.service.ReferenceService}.
 * The previous version called {@code app().dentists().findActive()} straight from
 * here, which is the web tier reaching into the data tier.</p>
 */
public class ReferenceApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            switch (request.getServletPath()) {
                case "/api/dentists" -> writeJson(response,
                        app().referenceService().activeDentists(currentUser(request)));
                case "/api/treatments" -> writeJson(response,
                        app().referenceService().activeTreatments(currentUser(request)));
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }
}
