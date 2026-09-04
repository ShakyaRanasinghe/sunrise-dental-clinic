package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Booking: choose a dentist, a treatment and a time.
 *
 * <p>Three steps on one screen, driven by the query string rather than by script, because
 * the stack has no client-side JavaScript. Choosing a dentist reloads the page with that
 * dentist's open times; choosing a time posts the booking. Each step is a real address, so
 * the back button works and a chosen time can be shared or bookmarked.</p>
 */
public class BookAppointmentServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String dentistId = field(request, "dentistId");
            LocalDate date = dateField(request, "date", LocalDate.now());
            String patientId = field(request, "patientId");

            request.setAttribute("dentists",
                    app().referenceService().activeDentists(currentUser(request)));
            // GAP-FTB-07: once a dentist is chosen, offer only the treatments they
            // perform; before that, show the full active catalogue.
            request.setAttribute("treatments",
                    dentistId != null && !dentistId.isBlank()
                            ? app().referenceService().treatmentsFor(currentUser(request), dentistId)
                            : app().referenceService().activeTreatments(currentUser(request)));
            request.setAttribute("dentistId", dentistId);
            request.setAttribute("date", date);
            request.setAttribute("patientId", patientId);
            request.setAttribute("serviceCharge",
                    app().clinicIdentity().serviceCharge());

            // FR-PAT-17: availability overview when no dentist is selected yet.
            if (dentistId == null || dentistId.isBlank()) {
                request.setAttribute("availabilityOverview",
                        app().slotService().patientAvailabilityOverview(14));
            }

            if (dentistId != null && !dentistId.isBlank()) {
                request.setAttribute("slots", app().slotService().openSlots(dentistId, date));
            }
            render(request, response, "appointments/book");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String patientId = field(request, "patientId");
            // GAP-FTB-06: an "Other (describe…)" booking sends no treatmentId but a
            // free-text reason; a named treatment sends a treatmentId and no reason.
            String treatmentId = field(request, "treatmentId");
            String patientReason = field(request, "patientReason");
            if (treatmentId == null && patientReason == null) {
                throw new IllegalArgumentException("Choose a treatment, or describe what the visit is for.");
            }
            AppointmentResponse booked = app().appointmentService().book(
                    currentUser(request),
                    requiredField(request, "slotId", "Time"),
                    treatmentId,
                    // Staff booking on a patient's behalf name them; a patient sending
                    // this is ignored, because the service resolves from the account.
                    patientId,
                    patientReason);

            // Reception booking on behalf: redirect to the printable slip.
            // Patient self-booking: redirect to their home page.
            if (patientId != null && !patientId.isBlank()) {
                redirect(request, response, "/reception/slip?appointmentNo=" + booked.appointmentNo());
            } else {
                redirect(request, response, "/patient/home?booked=" + booked.appointmentNo());
            }
        });
    }
}
