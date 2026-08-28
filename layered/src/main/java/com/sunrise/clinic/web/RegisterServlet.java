package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.UserAccount;
import com.sunrise.clinic.security.AuthenticationFilter;
import com.sunrise.clinic.security.ClinicPrincipal;
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "register");
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
            if (app().users().findByEmail(email).isPresent()) {
                throw new IllegalArgumentException(
                        "An account already exists for " + email + ". Try signing in instead.");
            }

            UserAccount created = registerAtomically(name, email, password, contactNumber, address, dob);

            // Sign them straight in — asking someone to log in immediately after
            // typing their password is friction with no security benefit.
            AuthenticationFilter.establishSession(request,
                    new ClinicPrincipal(created.getUid(), created.getDisplayName(), created.getRole()));
            redirect(request, response, AuthenticationFilter.homeFor(created.getRole()));
        });
    }

    private UserAccount registerAtomically(String name, String email, String password,
                                           String contactNumber, String address, LocalDate dob) {
        return app().transactionRunner().execute(() -> {
            UserAccount account = app().authService().register(email, password, name, Role.PATIENT);
            app().patients().save(Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .userUid(account.getUid())
                    .name(name)
                    .address(address)
                    .contactNumber(contactNumber)
                    .email(email)
                    .dob(dob)
                    .build());
            return account;
        });
    }
}
