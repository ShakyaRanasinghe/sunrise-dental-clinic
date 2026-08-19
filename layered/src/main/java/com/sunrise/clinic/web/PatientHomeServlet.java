package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The patient's own page: their upcoming and past appointments, with the option to
 * cancel one or book another.
 *
 * <p>Everything shown is scoped to the signed-in patient's own profile, looked up
 * from their account rather than taken from the request. A patient id in a query
 * string would be trivially editable, and would let anyone read anyone else's
 * appointment history.</p>
 */
public class PatientHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.PATIENT);

            Patient patient = app().patients().findByUserUid(user.uid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "We could not find your patient record. Please contact the front desk."));

            List<Appointment> appointments = app().appointments().findByPatientId(patient.getId());

            // Names for the ids the appointment rows carry, so the page can show
            // "Dr. Silva — Scaling" instead of two opaque identifiers.
            Map<String, String> dentistNames = app().dentists().findAll().stream()
                    .collect(Collectors.toMap(d -> d.getId(), d -> d.getName(), (a, b) -> a));
            Map<String, String> treatmentNames = app().treatments().findAll().stream()
                    .collect(Collectors.toMap(t -> t.getId(), t -> t.getName(), (a, b) -> a));

            LocalDate today = LocalDate.now();
            request.setAttribute("patient", patient);
            request.setAttribute("upcoming", appointments.stream()
                    .filter(a -> a.getDate() != null && !a.getDate().isBefore(today))
                    .sorted(java.util.Comparator.comparing(Appointment::getDate)
                            .thenComparing(Appointment::getTime))
                    .toList());
            request.setAttribute("past", appointments.stream()
                    .filter(a -> a.getDate() != null && a.getDate().isBefore(today))
                    .toList());
            request.setAttribute("dentistNames", dentistNames);
            request.setAttribute("treatmentNames", treatmentNames);
            request.setAttribute("bills", billsByAppointment(appointments));

            render(request, response, "patient/home");
        });
    }

    /** Cancel one of the patient's own appointments. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.PATIENT);

            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            Patient patient = app().patients().findByUserUid(user.uid())
                    .orElseThrow(() -> new ResourceNotFoundException("No patient record for your account."));
            Appointment appointment = app().appointmentService().findByNo(appointmentNo);

            if (!appointment.getPatientId().equals(patient.getId())) {
                throw new AccessControl.AccessDeniedException(
                        "You can only cancel your own appointments.");
            }

            app().appointmentService().cancel(appointmentNo);
            redirect(request, response, "/patient/home?cancelled=" + appointmentNo);
        });
    }

    private Map<String, Boolean> billsByAppointment(List<Appointment> appointments) {
        return appointments.stream().collect(Collectors.toMap(
                Appointment::getAppointmentNo,
                a -> app().bills().findByAppointmentNo(a.getAppointmentNo()).isPresent(),
                (a, b) -> a,
                java.util.LinkedHashMap::new));
    }
}
