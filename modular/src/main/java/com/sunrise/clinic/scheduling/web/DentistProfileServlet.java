package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The dentist's own profile (GAP-DEN-14) — name, specialisation and phone as the
 * public cards show them, editable from the dashboard like the patient's own
 * "My details".
 *
 * <p>Read-only by default with an Edit control revealing the editable subset
 * (GAP-FTB-14); a saved change confirms in a sub-window. The consultation fee is
 * shown but never editable here — it prices every bill, so only the
 * administrator sets it (GAP-ADM-02). Resolved from the signed-in account, so a
 * dentist reaches only their own record.</p>
 */
public class DentistProfileServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String action = requiredField(request, "action", "Action");
            if (!"update".equals(action)) {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
            app().referenceService().updateOwnDetails(currentUser(request),
                    currentUser(request).uid(),
                    requiredField(request, "name", "Name"),
                    field(request, "specialization"),
                    field(request, "phone"));
            // Redirect after post, so a refresh does not save the same edit twice.
            redirect(request, response, "/dentist/profile?saved=1");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("profile", app().referenceService()
                .ownProfile(currentUser(request).uid()).orElse(null));
        request.setAttribute("saved", field(request, "saved"));
        request.setAttribute("editing", "1".equals(request.getParameter("edit")));
        render(request, response, "scheduling/dentist-profile");
    }
}
