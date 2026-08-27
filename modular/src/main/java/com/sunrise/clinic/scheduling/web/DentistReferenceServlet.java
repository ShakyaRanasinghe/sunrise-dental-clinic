package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Read-only dentist quick-reference for reception.
 *
 * <p>Shows all active dentists with specialisation and consultation fee so
 * reception can advise a walk-in patient on which doctor to see without
 * recalling details from memory.</p>
 */
public class DentistReferenceServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            request.setAttribute("dentists",
                    app().referenceService().activeDentists(currentUser(request)));
            request.setAttribute("serviceCharge",
                    app().config().getDecimal("clinic.billing.service-charge", "200"));
            render(request, response, "scheduling/dentists");
        });
    }
}
