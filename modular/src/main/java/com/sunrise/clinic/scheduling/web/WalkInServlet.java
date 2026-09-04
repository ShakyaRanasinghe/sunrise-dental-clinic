package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Unified walk-in booking flow for reception.
 *
 * <p>Combines patient search/register and today's open slots on one screen so
 * reception can handle a walk-in from arrival to booked appointment without
 * switching between the Patients and Availability screens.</p>
 *
 * <p>Flow: search or register patient → confirm patient → see open slots →
 * click a slot to go to the booking page pre-filled for that patient.</p>
 */
public class WalkInServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String q = field(request, "q");
            String patientId = field(request, "patientId");
            LocalDate date = dateField(request, "date", LocalDate.now());

            String filterDentistId = field(request, "filterDentistId");

            request.setAttribute("q", q);
            request.setAttribute("date", date);
            request.setAttribute("patientId", patientId);
            request.setAttribute("filterDentistId", filterDentistId);
            request.setAttribute("dentists",
                    app().referenceService().activeDentists(currentUser(request)));
            request.setAttribute("serviceCharge",
                    app().clinicIdentity().serviceCharge());
            request.setAttribute("treatments",
                    app().referenceService().activeTreatments(currentUser(request)));

            if (q != null) {
                request.setAttribute("results",
                        app().patientService().search(currentUser(request), q));
            }
            if (patientId != null) {
                request.setAttribute("patient",
                        app().patientService().findById(currentUser(request), patientId));
                java.util.List<com.sunrise.clinic.scheduling.domain.SlotResponse> allSlots =
                        app().slotService().openSlotsBetween(date, date);
                // Apply dentist filter if selected
                if (filterDentistId != null && !filterDentistId.isBlank()) {
                    allSlots = allSlots.stream()
                            .filter(s -> filterDentistId.equals(s.dentistId()))
                            .toList();
                }
                request.setAttribute("slots", allSlots);
            }
            render(request, response, "scheduling/walkin");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            PatientService.Registration created = app().patientService().register(
                    currentUser(request),
                    new PatientService.NewPatient(
                            requiredField(request, "name", "Patient name"),
                            requiredField(request, "contactNumber", "Contact number"),
                            field(request, "address"),
                            field(request, "email"),
                            field(request, "dob")));
            // GAP-FTB-14: confirm the registration in a sub-window on return.
            redirect(request, response,
                    "/reception/walkin?patientId=" + created.patient().id() + "&registered=1");
        });
    }
}
