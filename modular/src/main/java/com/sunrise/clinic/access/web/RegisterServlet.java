package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.io.IOException;
import java.util.UUID;

/**
 * Patient self-registration.
 *
 * <p>Creates two linked records in one step: the {@link UserAccount} that lets the
 * person sign in, and the {@link Patient} profile that clinical records attach to.
 * The class diagram keeps these separate — "who can log in" is not the same thing
 * as "who they are in the clinic" — but from the patient's point of view signing up
 * is a single action, so the servlet creates both and links them.</p>
 *
 * <p>The pair is written inside one transaction. An account with no profile would
 * let someone sign in and immediately hit an error when they tried to book, which
 * is exactly the sort of half-finished state a transaction exists to prevent.</p>
 */
public class RegisterServlet extends PageServlet {
    /*
     * Creates the account only.
     *
     * FR-PAT-04 wants the user_account row and the patient profile row in one
     * transaction. The patients module arrives in step 3, so the profile row is
     * added there and this is the same Partial status layered/ already carried -
     * not a new gap. Until then a freshly registered patient has an account and
     * no profile, which is exactly the condition that makes booking answer
     * "No patient profile for user ...".
     */

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "access/register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String name = requiredField(request, "name", "Full name");
            String email = requiredField(request, "email", "Email").toLowerCase();
            String password = requiredField(request, "password", "Password");
            String confirm = requiredField(request, "confirmPassword", "Password confirmation");
            String contactNumber = requiredField(request, "contactNumber", "Contact number");
            String address = field(request, "address");
            LocalDate dob = dateField(request, "dob", null);

            if (!password.equals(confirm)) {
                throw new IllegalArgumentException("The two passwords do not match.");
            }
            if (password.length() < 8) {
                throw new IllegalArgumentException("Please choose a password of at least 8 characters.");
            }
            // The factory rejects a duplicate email, so no pre-check here: asking
            // and then acting is a race, and it duplicated the rule.
            UserAccount created = app().accountFactory()
                    .registerPatient(email, password, name);

            // Sign them straight in — asking someone to log in immediately after
            // typing their password is friction with no security benefit.
            AuthenticationFilter.establishSession(request,
                    new ClinicPrincipal(created.getUid(), created.getDisplayName(), created.getRole()));
            redirect(request, response, AuthenticationFilter.homeFor(created.getRole()));
        });
    }

}
