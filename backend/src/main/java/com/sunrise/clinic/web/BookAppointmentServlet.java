package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * The booking screen — the system's most important workflow, and the one the
 * sequence diagram documents.
 *
 * <p>{@code GET} shows the available slots for a chosen dentist and day;
 * {@code POST} books the selected one. Both patients (for themselves) and
 * receptionists (for a walk-in or telephone booking) use this page, which is why
 * the use case model shows those two actors sharing the same included steps.</p>
 *
 * <p>After a successful booking the servlet redirects rather than rendering. That
 * is the Post/Redirect/Get pattern, and here it is doing real work: without it, a
 * patient who refreshed the confirmation page would re-submit the form and try to
 * book a second appointment.</p>
 */
public class BookAppointmentServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.PATIENT, Role.RECEPTIONIST, Role.ADMIN);

            LocalDate date = dateField(request, "date", LocalDate.now());
            String dentistId = field(request, "dentistId");

            request.setAttribute("dentists", app().dentists().findActive());
            request.setAttribute("treatments", app().treatments().findActive());
            request.setAttribute("date", date);
            request.setAttribute("dentistId", dentistId);

            if (dentistId != null) {
                List<Slot> open = app().slotService().openSlots(dentistId, date);
                request.setAttribute("slots", open);
                request.setAttribute("noSlots", open.isEmpty());
            }

            // A receptionist books on someone's behalf, so they need to pick a patient.
            if (user.role() != Role.PATIENT) {
                request.setAttribute("patients", app().patients().findAll());
            }

            render(request, response, "patient/book");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.PATIENT, Role.RECEPTIONIST, Role.ADMIN);

            String slotId = requiredField(request, "slotId", "A time slot");
            String treatmentId = requiredField(request, "treatmentId", "A treatment");
            String patientId = resolvePatientId(request, user);

            Appointment appointment = app().appointmentService()
                    .book(patientId, slotId, treatmentId, user.uid(), user.role());

            String destination = user.role() == Role.PATIENT ? "/patient/home" : "/reception/home";
            redirect(request, response, destination + "?booked=" + appointment.getAppointmentNo());
        });
    }

    /**
     * Whose appointment is this?
     *
     * <p>A patient always books for themselves — the id comes from their own
     * profile and any {@code patientId} in the form is ignored, so the field cannot
     * be tampered with to book in someone else's name. Staff must choose one.</p>
     */
    private String resolvePatientId(HttpServletRequest request, ClinicPrincipal user) {
        if (user.role() == Role.PATIENT) {
            return app().patients().findByUserUid(user.uid())
                    .map(Patient::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "We could not find your patient record. Please contact the front desk."));
        }
        return requiredField(request, "patientId", "A patient");
    }
}
