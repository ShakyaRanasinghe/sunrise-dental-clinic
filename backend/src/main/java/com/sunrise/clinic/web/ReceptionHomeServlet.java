package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.security.AccessControl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The front desk's day view: every appointment for a chosen date, with who the
 * patient is, which dentist they are seeing, and whether they have been billed yet.
 *
 * <p>This is the receptionist's working screen, so it defaults to today and shows
 * cancelled appointments alongside the live ones — the front desk needs to know a
 * slot was released, not have it silently vanish.</p>
 */
public class ReceptionHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);

            LocalDate date = dateField(request, "date", LocalDate.now());
            List<Appointment> today = app().appointments().findByDate(date);

            Map<String, String> patientNames = app().patients().findAll().stream()
                    .collect(Collectors.toMap(p -> p.getId(), p -> p.getName(), (a, b) -> a));
            Map<String, String> dentistNames = app().dentists().findAll().stream()
                    .collect(Collectors.toMap(d -> d.getId(), d -> d.getName(), (a, b) -> a));
            Map<String, String> treatmentNames = app().treatments().findAll().stream()
                    .collect(Collectors.toMap(t -> t.getId(), t -> t.getName(), (a, b) -> a));
            Map<String, Boolean> billed = today.stream().collect(Collectors.toMap(
                    Appointment::getAppointmentNo,
                    a -> app().bills().findByAppointmentNo(a.getAppointmentNo()).isPresent(),
                    (a, b) -> a));

            request.setAttribute("date", date);
            request.setAttribute("appointments", today);
            request.setAttribute("patientNames", patientNames);
            request.setAttribute("dentistNames", dentistNames);
            request.setAttribute("treatmentNames", treatmentNames);
            request.setAttribute("billed", billed);

            render(request, response, "reception/home");
        });
    }

    /** Cancel an appointment on the patient's behalf (a phone call to the desk). */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            String date = field(request, "date");
            app().appointmentService().cancel(appointmentNo);
            redirect(request, response, "/reception/home?cancelled=" + appointmentNo
                    + (date == null ? "" : "&date=" + date));
        });
    }
}
