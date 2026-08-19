package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.security.AccessControl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The front desk's patient register: search existing records, and add a new
 * patient for someone who walked in or telephoned.
 *
 * <p>A patient created here has no {@code userUid}, because there is no online
 * account behind them — they exist in the clinic's records without ever having used
 * the portal. That is exactly why the class diagram makes the link from
 * {@code Patient} to {@code UserAccount} optional.</p>
 */
public class PatientRecordsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);

            String query = field(request, "q");
            List<Patient> results = query == null
                    ? app().patients().findAll()
                    : app().patients().search(query);

            request.setAttribute("q", query);
            request.setAttribute("patients", results);
            render(request, response, "reception/patients");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);

            String name = requiredField(request, "name", "Patient name");
            String contactNumber = requiredField(request, "contactNumber", "Contact number");
            String email = field(request, "email");
            String address = field(request, "address");
            LocalDate dob = dateField(request, "dob", null);

            if (email != null && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }

            Patient patient = Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .name(name)
                    .contactNumber(contactNumber)
                    .email(email)
                    .address(address)
                    .dob(dob)
                    .build();
            app().patients().save(patient);

            redirect(request, response, "/reception/patients?registered="
                    + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8));
        });
    }
}
