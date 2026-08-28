package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.json.Json;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Patient records.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code POST /api/patients}</td><td>register a patient</td></tr>
 *   <tr><td>{@code GET  /api/patients/{id}}</td><td>fetch one</td></tr>
 *   <tr><td>{@code GET  /api/patients?q=}</td><td>front-desk search</td></tr>
 * </table>
 *
 * <p>Patient records are personal data, so reads are restricted to clinic staff and
 * to the patient themselves. Only the search and lookup routes need that check —
 * registration is how a new patient gets a record in the first place.</p>
 */
public class PatientApiServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> register(request, response));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                search(request, response);
            } else if (path.size() == 1) {
                get(request, response, path.get(0));
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClinicPrincipal user = currentUser(request);
        Map<String, Object> body = readBody(request);

        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .userUid(user != null ? user.uid() : null)
                .name(required(body, "name"))
                .address(Json.string(body, "address"))
                .contactNumber(required(body, "contactNumber"))
                .email(validEmail(Json.string(body, "email")))
                .dob(optionalDate(Json.string(body, "dob")))
                .build();

        app().patients().save(patient);
        writeJson(response, HttpServletResponse.SC_CREATED, patient);
    }

    private void get(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        ClinicPrincipal user = currentUser(request);
        Patient patient = app().patients().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        // Staff may read any record; a patient may read only their own.
        AccessControl.requireSelfOr(user, patient.getUserUid(),
                Role.RECEPTIONIST, Role.ADMIN, Role.DENTIST);
        writeJson(response, patient);
    }

    private void search(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);
        String term = request.getParameter("q");
        List<Patient> results = (term == null || term.isBlank())
                ? app().patients().findAll()
                : app().patients().search(term.trim());
        writeJson(response, results);
    }

    private static String required(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String validEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        // Deliberately permissive: enough to catch a typo, not so strict that it
        // rejects a legitimate address.
        if (!trimmed.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return trimmed;
    }

    private static LocalDate optionalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("dob must be a date in yyyy-MM-dd format");
        }
    }
}
