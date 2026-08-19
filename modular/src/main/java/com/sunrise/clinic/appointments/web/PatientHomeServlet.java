package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The patient's own dashboard: their appointments, and the way to book another.
 *
 * <p>Replaces the step-2 stub at {@code /patient/home}.</p>
 */
public class PatientHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            // No id from the request: a patient's own appointments are resolved from
            // their account, so there is no parameter to tamper with.
            request.setAttribute("appointments",
                    app().appointmentService().forSelf(currentUser(request)));
            request.setAttribute("booked", field(request, "booked"));
            request.setAttribute("cancelled", field(request, "cancelled"));
            render(request, response, "appointments/patient-home");
        });
    }

    /** Cancelling from the dashboard. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            app().appointmentService().cancel(currentUser(request), appointmentNo);
            redirect(request, response, "/patient/home?cancelled=" + appointmentNo);
        });
    }
}
