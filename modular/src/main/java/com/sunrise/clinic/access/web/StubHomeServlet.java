package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * TEMPORARY - delete when the reporting module lands.
 *
 * <p>Stood in for all four role landing pages from step 2. Three of them are now real
 * screens, so this is down to one: {@code /admin/reports}, which is the administrator's
 * home path and belongs to the reporting module. Without it, signing in as an
 * administrator would redirect to a 404.</p>
 *
 * <p>Kept rather than pointing the administrator somewhere else, because
 * {@code AdminPolicy.homePath()} is the single statement of where that role lands, and
 * changing it to a temporary destination would make the policy lie.</p>
 */
public class StubHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> render(request, response, "shared/stub-home"));
    }
}
