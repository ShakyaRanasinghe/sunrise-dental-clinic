package com.sunrise.clinic.patients.web;

import com.sunrise.clinic.patients.domain.NoteCategory;
import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The patient's own profile, and where they declare what a dentist should know.
 *
 * <p>Registration collects none of this (FR-NOTE-02): sign-up is name, contact, email and
 * password. Medical information is declared here, afterwards, by someone who has already
 * decided to use the clinic - which is both kinder and the only way to keep the sign-up form
 * short enough to finish.</p>
 */
public class PatientProfileServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response, null));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String action = requiredField(request, "action", "Action");
            switch (action) {
                case "update" -> app().patientService().updateOwn(currentUser(request),
                        new PatientService.ProfileUpdate(
                                requiredField(request, "name", "Name"),
                                field(request, "address"),
                                requiredField(request, "contactNumber", "Contact number"),
                                field(request, "dob")));
                case "declare" -> app().patientNoteService().declare(currentUser(request),
                        category(request), requiredField(request, "detail", "Detail"),
                        request.getParameter("critical") != null);
                case "amend" -> app().patientNoteService().amend(currentUser(request),
                        requiredField(request, "noteId", "Note"),
                        category(request), requiredField(request, "detail", "Detail"),
                        request.getParameter("critical") != null);
                case "withdraw" -> app().patientNoteService().withdraw(currentUser(request),
                        requiredField(request, "noteId", "Note"));
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
            // Redirect after post, so a refresh does not declare the same note twice.
            redirect(request, response, "/patient/profile?saved=1");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      String message) throws ServletException, IOException {
        request.setAttribute("profile", app().patientService().findOwn(currentUser(request))
                .orElse(null));
        request.setAttribute("notes", app().patientNoteService().own(currentUser(request)));
        request.setAttribute("categories", NoteCategory.values());
        request.setAttribute("saved", field(request, "saved"));
        request.setAttribute("editing", "1".equals(request.getParameter("edit")));
        request.setAttribute("message", message);
        render(request, response, "patients/profile");
    }

    private static NoteCategory category(HttpServletRequest request) {
        String raw = request.getParameter("category");
        try {
            return NoteCategory.valueOf(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Choose what kind of note this is.");
        }
    }
}
