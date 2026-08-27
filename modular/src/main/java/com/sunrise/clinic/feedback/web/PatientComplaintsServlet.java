package com.sunrise.clinic.feedback.web;

import com.sunrise.clinic.feedback.domain.ComplaintCategory;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Where a patient raises a concern, and sees what happened to the ones they raised.
 *
 * <p>The form says who will read it and who will not (FR-CMP-12). A patient deciding whether
 * to complain about the person who is going to treat them next month deserves to know that
 * the dentist will not see it before they decide, not afterwards.</p>
 */
public class PatientComplaintsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            app().complaintService().raise(currentUser(request),
                    requiredField(request, "dentistId", "Dentist"),
                    field(request, "appointmentNo"),
                    category(request),
                    requiredField(request, "detail", "What happened"));
            redirect(request, response, "/patient/complaints?raised=1");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("complaints", app().complaintService().own(currentUser(request)));
        // Only dentists this patient has actually seen, so the form cannot name a stranger.
        request.setAttribute("appointments",
                app().appointmentService().forSelf(currentUser(request)));
        request.setAttribute("categories", ComplaintCategory.values());
        request.setAttribute("raised", field(request, "raised"));
        render(request, response, "feedback/patient-complaints");
    }

    private static ComplaintCategory category(HttpServletRequest request) {
        String raw = request.getParameter("category");
        try {
            return ComplaintCategory.valueOf(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Choose what your concern is about.");
        }
    }
}
