package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The dentist's own diary, and where they record what they did.
 *
 * <p>A dentist sees only their own appointments — the list is filtered by the
 * dentist record linked to their account, not by anything in the request. Marking a
 * treatment complete captures the diagnosis, which is clinical data and therefore
 * only ever visible to this dentist and to the patient.</p>
 */
public class DentistScheduleServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.DENTIST);

            Dentist dentist = ownProfile(user);
            LocalDate date = dateField(request, "date", LocalDate.now());

            List<Appointment> mine = app().appointments().findByDentistId(dentist.getId()).stream()
                    .filter(a -> date.equals(a.getDate()))
                    .sorted(Comparator.comparing(Appointment::getTime))
                    .toList();

            Map<String, String> patientNames = app().patients().findAll().stream()
                    .collect(Collectors.toMap(p -> p.getId(), p -> p.getName(), (a, b) -> a));
            Map<String, String> treatmentNames = app().treatments().findAll().stream()
                    .collect(Collectors.toMap(t -> t.getId(), t -> t.getName(), (a, b) -> a));

            request.setAttribute("dentist", dentist);
            request.setAttribute("date", date);
            request.setAttribute("appointments", mine);
            request.setAttribute("patientNames", patientNames);
            request.setAttribute("treatmentNames", treatmentNames);

            render(request, response, "dentist/schedule");
        });
    }

    /** Record the diagnosis and mark the appointment completed. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.DENTIST);

            Dentist dentist = ownProfile(user);
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            String diagnosis = requiredField(request, "diagnosis", "Diagnosis");

            Appointment appointment = app().appointmentService().findByNo(appointmentNo);
            if (!dentist.getId().equals(appointment.getDentistId())) {
                throw new AccessControl.AccessDeniedException(
                        "You can only complete your own appointments.");
            }

            app().appointmentService().complete(appointmentNo, diagnosis);
            String date = field(request, "date");
            redirect(request, response, "/dentist/schedule?completed=" + appointmentNo
                    + (date == null ? "" : "&date=" + date));
        });
    }

    private Dentist ownProfile(ClinicPrincipal user) {
        return app().dentists().findByUserUid(user.uid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Your account is not linked to a dentist record. Please ask an administrator."));
    }
}
