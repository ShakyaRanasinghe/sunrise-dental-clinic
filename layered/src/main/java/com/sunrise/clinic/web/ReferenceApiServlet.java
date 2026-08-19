package com.sunrise.clinic.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Read-only reference data the booking screens need: the list of practising
 * dentists and the treatment catalogue.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET /api/dentists}</td><td>active dentists</td></tr>
 *   <tr><td>{@code GET /api/treatments}</td><td>active treatments</td></tr>
 * </table>
 *
 * <p>Both are served by one servlet mapped to two URLs, since they are the same
 * kind of request — a small, cacheable list any signed-in user may read.</p>
 */
public class ReferenceApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            String servletPath = request.getServletPath();
            switch (servletPath) {
                case "/api/dentists" -> writeJson(response, app().dentists().findActive());
                case "/api/treatments" -> writeJson(response, app().treatments().findActive());
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }
}
