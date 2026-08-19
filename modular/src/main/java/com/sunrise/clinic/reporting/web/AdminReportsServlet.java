package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * The administrator's reports - and the screen that finally shows the owner where the money
 * goes.
 *
 * <p>Replaces the last stub landing page. Until this existed, the revenue split was written
 * on every bill and permission-gated to the administrator, and there was nowhere to see it -
 * a revenue model that is a database column rather than a feature.</p>
 */
public class AdminReportsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            LocalDate from = dateField(request, "from", app().reportService().defaultFrom());
            LocalDate to = dateField(request, "to", app().reportService().today());

            request.setAttribute("from", from);
            request.setAttribute("to", to);
            request.setAttribute("report",
                    app().reportService().forPeriod(currentUser(request), from, to));
            render(request, response, "reporting/reports");
        });
    }
}
