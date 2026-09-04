package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            // GAP-ADM-13: sortable table — time order by default, as before.
            String sort = field(request, "sort");
            if (!"time_desc".equals(sort) && !"dentist".equals(sort)) {
                sort = "time_asc";
            }
            List<AppointmentResponse> rows =
                    new ArrayList<>(app().appointmentService().onDate(currentUser(request), date));
            rows.sort(switch (sort) {
                case "time_desc" -> Comparator.comparing(AppointmentResponse::time).reversed();
                case "dentist" -> Comparator
                        .comparing((AppointmentResponse a) ->
                                a.dentistName() == null ? "" : a.dentistName())
                        .thenComparing(AppointmentResponse::time);
                default -> Comparator.comparing(AppointmentResponse::time);
            });
            request.setAttribute("date", date);
            request.setAttribute("sort", sort);
            request.setAttribute("appointments", rows);
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
            String sort = field(request, "sort");
            app().appointmentService().cancel(currentUser(request), appointmentNo);
            redirect(request, response, "/reception/home?date=" + date + "&cancelled=" + appointmentNo
                    + (sort == null || sort.isBlank() ? "" : "&sort=" + sort.trim()));
        });
    }
}
