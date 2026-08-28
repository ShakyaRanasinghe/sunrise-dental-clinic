package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * The front desk's day view: everything booked on one date, across every dentist.
 *
 * <p>Replaces the step-2 stub at {@code /reception/home}. Reception sees the patient's
 * name, the dentist and the treatment - and no diagnosis, because
 * {@code AppointmentResponse} has no field for one.</p>
 */
public class ReceptionDayServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            LocalDate date = dateField(request, "date", LocalDate.now());
            request.setAttribute("date", date);
            request.setAttribute("appointments",
                    app().appointmentService().onDate(currentUser(request), date));
            request.setAttribute("cancelled", field(request, "cancelled"));
            render(request, response, "appointments/reception-day");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            LocalDate date = dateField(request, "date", LocalDate.now());
            app().appointmentService().cancel(currentUser(request), appointmentNo);
            redirect(request, response, "/reception/home?date=" + date + "&cancelled=" + appointmentNo);
        });
    }
}
