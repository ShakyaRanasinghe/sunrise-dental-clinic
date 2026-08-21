package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Treatment catalogue administration: add, edit pricing, activate/deactivate.
 *
 * <p>All actions require {@code MANAGE_TREATMENTS} — only the administrator reaches here.
 * Deactivating a treatment removes it from the patient booking dropdown immediately
 * without touching any appointment history that references it.</p>
 */
public class TreatmentAdminServlet extends PageServlet {

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
                case "add" -> app().treatmentAdminService().add(
                        currentUser(request),
                        requiredField(request, "name", "Name"),
                        field(request, "description"),
                        cost(request));
                case "update" -> app().treatmentAdminService().update(
                        currentUser(request),
                        requiredField(request, "id", "Treatment"),
                        requiredField(request, "name", "Name"),
                        field(request, "description"),
                        cost(request));
                case "deactivate" -> app().treatmentAdminService().setActive(
                        currentUser(request),
                        requiredField(request, "id", "Treatment"), false);
                case "reactivate" -> app().treatmentAdminService().setActive(
                        currentUser(request),
                        requiredField(request, "id", "Treatment"), true);
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
            redirect(request, response, "/admin/treatments");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response, String notice)
            throws ServletException, IOException {
        request.setAttribute("treatments",
                app().treatmentAdminService().listAll(currentUser(request)));
        if (notice != null) {
            request.setAttribute("notice", notice);
        }
        render(request, response, "reporting/treatments");
    }

    private static BigDecimal cost(HttpServletRequest request) {
        String raw = request.getParameter("baseCost");
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Price is required.");
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price cannot be negative.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a number, like 4500.00");
        }
    }
}
