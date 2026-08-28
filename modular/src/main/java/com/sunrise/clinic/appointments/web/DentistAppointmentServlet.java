package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.appointments.domain.AppointmentDetailResponse;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * One appointment as its treating dentist sees it: the visit, and what the patient has
 * declared - FR-NOTE-07.
 *
 * <p>The notes are on this page rather than behind a further click, because a dentist about
 * to treat somebody should not have to know to go and look for an allergy.</p>
 *
 * <p>No role check here. {@code findDetail} returns an {@link AppointmentDetailResponse} only
 * to the treating dentist or the patient, and refuses another dentist outright - so the type
 * that comes back is itself the answer to "may this caller see this". A receptionist reaching
 * this URL gets the plain response, which has no field for a diagnosis or a note.</p>
 */
public class DentistAppointmentServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            Object detail = app().appointmentService()
                    .findDetail(currentUser(request), appointmentNo);

            if (!(detail instanceof AppointmentDetailResponse clinical)) {
                // Reached by somebody who may read the appointment but not its clinical
                // detail. Nothing to show on a clinical page.
                throw new com.sunrise.clinic.access.service.AccessControl.AccessDeniedException(
                        "That appointment's clinical detail is not yours to read.");
            }
            request.setAttribute("appointment", clinical);
            render(request, response, "appointments/dentist-appointment");
        });
    }
}
