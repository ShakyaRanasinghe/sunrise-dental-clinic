package com.sunrise.clinic.patients.web;

import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The front desk's patient register: search existing records, and register someone
 * who walked in or telephoned.
 *
 * <p>A patient registered here has no portal account, which is the ordinary case at
 * a clinic - the record exists because they were treated, not because they signed
 * up. Their {@code userUid} stays null until they register themselves.</p>
 */
public class PatientRecordsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String query = field(request, "q");
            request.setAttribute("q", query);
            request.setAttribute("patients",
                    app().patientService().search(currentUser(request), query));
            // Set by the redirect after a successful registration. Resolved back into
            // the record rather than trusted as text, so the confirmation names the
            // patient and the duplicate warning is still right after a refresh.
            // ?edit=<id> puts one row into a form. A real address, so the back button works
            // and the desk can be told "open this patient" over the telephone.
            String editing = field(request, "edit");
            if (editing != null) {
                request.setAttribute("editing",
                        app().patientService().findById(currentUser(request), editing));
            }
            String registeredId = field(request, "registered");
            if (registeredId != null) {
                request.setAttribute("registered",
                        app().patientService().findById(currentUser(request), registeredId));
                request.setAttribute("possibleDuplicates",
                        app().patientService().possibleDuplicatesOf(currentUser(request), registeredId));
            }
            render(request, response, "patients/register");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            // Correcting and registering are the same form and the same fields; the id says
            // which one this is. One handler, so the validation cannot drift between them.
            String correcting = field(request, "patientId");
            if (correcting != null) {
                PatientService.Registration corrected = app().patientService().correct(
                        currentUser(request), correcting, formDetails(request));
                redirect(request, response,
                        "/reception/patients?registered=" + corrected.patient().id());
                return;
            }
            PatientService.Registration created = app().patientService().register(
                    currentUser(request), formDetails(request));

            // Redirect after post, so a refresh does not register the patient twice.
            // Only the id travels in the query string; the screen resolves it.
            redirect(request, response, "/reception/patients?registered=" + created.patient().id());
        });
    }

    /**
     * The five fields the form carries, whether it is registering or correcting.
     *
     * <p>Shared so the validation cannot drift between the two. They ask for the same things
     * and refuse the same things, and the only difference is whether an id came with them.</p>
     */
    private PatientService.NewPatient formDetails(HttpServletRequest request) {
        return new PatientService.NewPatient(
                requiredField(request, "name", "Patient name"),
                requiredField(request, "contactNumber", "Contact number"),
                field(request, "address"),
                field(request, "email"),
                field(request, "dob"));
    }
}
