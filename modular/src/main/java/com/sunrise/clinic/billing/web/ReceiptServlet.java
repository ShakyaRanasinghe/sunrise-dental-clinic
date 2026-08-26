package com.sunrise.clinic.billing.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The printable receipt - FR-REC-52.
 *
 * <p>A page rather than a generated PDF. The stack has no PDF library and would not gain
 * one for this: a print stylesheet gives the clinic a receipt on paper from any browser,
 * and gives the patient the same thing as a saved page. Adding a dependency to produce a
 * document the browser can already produce is a cost with no return.</p>
 *
 * <p>Reached by both the front desk and the patient. Who may see it is decided by
 * {@code BillingService.forAppointment} - a bill states what a named person paid for which
 * treatment, so it is not readable by whoever happens to hold the number.</p>
 */
public class ReceiptServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            request.setAttribute("bill",
                    app().billingService().forAppointment(currentUser(request), appointmentNo));
            request.setAttribute("clinicName", app().clinicIdentity().get("clinic.name"));
            request.setAttribute("clinicPhone", app().clinicIdentity().get("clinic.phone"));
            render(request, response, "billing/receipt");
        });
    }
}
