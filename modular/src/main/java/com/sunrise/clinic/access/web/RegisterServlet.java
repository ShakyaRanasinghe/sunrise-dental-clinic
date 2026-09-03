package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.patients.service.SelfRegistrationService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Patient self-registration — the only public write endpoint in the system (FR-PAT-07).
 *
 * <p>Reads the form and hands it to {@link SelfRegistrationService}, which creates the
 * {@link UserAccount} and the patient profile together in one transaction. This class validates
 * nothing itself, which is the fix for how it went wrong: it used to require a
 * {@code contactNumber} the form never asked for, so registration answered <b>400 "Contact
 * number is required"</b> whatever anybody typed — and it never created the patient profile at
 * all, so an account that did get made could sign in and then fail at the first thing it tried.
 * Both are gone because the rules now live in one place that a test can reach.</p>
 */
public class RegisterServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "access/register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            try {
                registerAndSignIn(request, response);
            } catch (IllegalArgumentException problem) {
                // Something the person can fix by retyping. Re-render the form
                // with the reason above the fields and everything they already
                // typed still in place — the view re-reads the submitted request
                // parameters — so a correction is one change, not a full retype,
                // and a request with invalid values is never accepted. Any other
                // failure keeps page()'s ordinary handling below.
                request.setAttribute("error", problem.getMessage());
                render(request, response, "access/register");
            }
        });
    }

    private void registerAndSignIn(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SelfRegistrationService.Registered registered = app().selfRegistrationService()
                .register(new SelfRegistrationService.Registration(
                        field(request, "name"),
                        field(request, "email"),
                        field(request, "password"),
                        field(request, "confirmPassword"),
                        // Required since GAP-PAT-20; the service enforces it (blank is refused).
                        field(request, "contactNumber"),
                        // Optional. The service says why.
                        field(request, "address"),
                        field(request, "dob")));

        // Sign them straight in — asking somebody to log in immediately after typing
        // their password is friction with no security benefit.
        UserAccount account = registered.account();
        AuthenticationFilter.establishSession(request, new ClinicPrincipal(
                account.getUid(), account.getDisplayName(), account.getRole()));
        redirect(request, response, AuthenticationFilter.homeFor(account.getRole()));
    }
}
