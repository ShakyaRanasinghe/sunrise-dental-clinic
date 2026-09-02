package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;

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
            request.setAttribute("today", LocalDate.now());
            // Rows carrying a critical-notes flag, not bare appointments - FR-NOTE-08.
            var day = app().appointmentService().forDentistWithWarnings(currentUser(request), date);
            // Pending (still to treat) first, in the order the day runs; what is finished
            // or cancelled is pushed below so a completed day does not lead the screen.
            var pending = day.stream()
                    .filter(row -> row.appointment().status() == AppointmentStatus.CONFIRMED)
                    .sorted(Comparator.comparing(row -> row.appointment().time()))
                    .toList();
            var done = day.stream()
                    .filter(row -> row.appointment().status() != AppointmentStatus.CONFIRMED)
                    .sorted(Comparator.comparing(row -> row.appointment().time()))
                    .toList();
            request.setAttribute("pending", pending);
            request.setAttribute("done", done);
            // The week ahead above the picker: today and six more days, so a booking made
            // for any day this week is visible without picking that day first - FR-DEN-15.
            var week = app().appointmentService().forDentistWeekWithWarnings(
                    currentUser(request), date, date.plusDays(6));
            request.setAttribute("week", week);
            request.setAttribute("hasWeekAppointments", week.stream()
                    .anyMatch(d -> !d.appointments().isEmpty()));
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
