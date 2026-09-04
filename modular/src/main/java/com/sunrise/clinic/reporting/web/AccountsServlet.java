package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.platform.web.PageServlet;
import com.sunrise.clinic.reporting.service.AccountAdminService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Account administration: create staff, unlock, deactivate, reset a password.
 *
 * <p>The one-time password for a newly created account is shown <b>once</b>, on the response
 * to the request that created it, and is never stored (FR-ADM-25). That is why this action
 * renders rather than redirecting: a redirect would either lose the password or have to
 * carry it in a URL, where it would sit in the browser history and the access log.</p>
 */
public class AccountsServlet extends PageServlet {

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
            AccountAdminService accounts = app().accountAdminService();
            AccountAdminService.NewAccount created = null;

            switch (action) {
                case "create" -> created = accounts.createStaff(currentUser(request),
                        requiredField(request, "email", "Email"),
                        requiredField(request, "displayName", "Name"),
                        role(request),
                        field(request, "specialization"),
                        fee(request),
                        requiredField(request, "username", "Username"));
                case "unlock" -> accounts.unlock(currentUser(request),
                        requiredField(request, "uid", "Account"));
                case "deactivate" -> accounts.setActive(currentUser(request),
                        requiredField(request, "uid", "Account"), false);
                case "reactivate" -> accounts.setActive(currentUser(request),
                        requiredField(request, "uid", "Account"), true);
                case "reset" -> created = accounts.resetPassword(currentUser(request),
                        requiredField(request, "uid", "Account"));
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
            show(request, response, created);
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      AccountAdminService.NewAccount created)
            throws ServletException, IOException {
        request.setAttribute("accounts", app().accountAdminService().list(currentUser(request)));
        request.setAttribute("created", created);
        request.setAttribute("roles", java.util.List.of(
                Role.RECEPTIONIST, Role.DENTIST, Role.ADMIN));
        render(request, response, "reporting/accounts");
    }

    private static Role role(HttpServletRequest request) {
        String raw = request.getParameter("role");
        try {
            Role role = Role.valueOf(raw == null ? "" : raw.trim());
            if (role == Role.PATIENT) {
                // The factory refuses it too; saying so here names the reason.
                throw new IllegalArgumentException(
                        "Patients register themselves. An administrator does not create them.");
            }
            return role;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Patients")) {
                throw e;
            }
            throw new IllegalArgumentException("Choose a role for the new account.");
        }
    }

    private static BigDecimal fee(HttpServletRequest request) {
        String raw = request.getParameter("consultationFee");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The consultation fee must be an amount, like 1500.00");
        }
    }
}
