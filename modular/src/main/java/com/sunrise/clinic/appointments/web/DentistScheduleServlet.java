package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * A dentist's own schedule, and where a diagnosis is recorded.
 *
 * <p>Replaces the step-2 stub at {@code /dentist/schedule}. The dentist is resolved from
 * the signed-in account, not from a parameter, so this screen cannot be pointed at another
 * dentist's day.</p>
 */
public class DentistScheduleServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            LocalDate date = dateField(request, "date", LocalDate.now());
            request.setAttribute("date", date);
            // Rows carrying a critical-notes flag, not bare appointments - FR-NOTE-08.
            request.setAttribute("appointments",
                    app().appointmentService().forDentistWithWarnings(currentUser(request), date));
            request.setAttribute("completed", field(request, "completed"));
            render(request, response, "appointments/dentist-schedule");
        });
    }

    /** Recording the diagnosis. The service checks the appointment is this dentist's. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            LocalDate date = dateField(request, "date", LocalDate.now());
            app().appointmentService().complete(currentUser(request), appointmentNo,
                    requiredField(request, "diagnosis", "Diagnosis"));
            redirect(request, response,
                    "/dentist/schedule?date=" + date + "&completed=" + appointmentNo);
        });
    }
}
