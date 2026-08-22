package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Printable appointment confirmation slip for walk-in patients.
 *
 * <p>Reached by reception after booking on a patient's behalf. Shows the appointment
 * details in a print-friendly layout the patient can take away — separate from the
 * billing receipt which is only issued after treatment.</p>
 */
public class AppointmentSlipServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            request.setAttribute("appointment",
                    app().appointmentService().findByNo(currentUser(request), appointmentNo));
            request.setAttribute("clinicPhone",
                    app().config().get("clinic.phone", ""));
            request.setAttribute("clinicAddress",
                    app().config().get("clinic.address", ""));
            render(request, response, "appointments/slip");
        });
    }
}
