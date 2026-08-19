package com.sunrise.clinic.patients.web;

import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Patient records as JSON.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET  /api/patients?q=}</td><td>search or list the register</td></tr>
 *   <tr><td>{@code GET  /api/patients/{id}}</td><td>one record</td></tr>
 *   <tr><td>{@code POST /api/patients}</td><td>register a walk-in</td></tr>
 * </table>
 *
 * <p>Every route delegates to {@link PatientService}, which holds the permission
 * check and the validation. This class reads the request and writes the response and
 * does nothing else - so the two defects the previous version carried, both of which
 * lived in an inline {@code register} method here, have nowhere to live now.</p>
 *
 * <p>Medical notes are also under {@code /api/patients/{id}/notes} in the contract.
 * They arrive with the {@code feedback} module in step 8; this class rejects the path
 * rather than pretending to serve it.</p>
 */
public class PatientApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                writeJson(response, app().patientService()
                        .search(currentUser(request), request.getParameter("q")));
            } else if (path.size() == 1) {
                writeJson(response, app().patientService()
                        .findById(currentUser(request), path.get(0)));
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            if (!pathParts(request).isEmpty()) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            Map<String, Object> body = readBody(request);
            PatientService.Registration created = app().patientService().register(
                    currentUser(request),
                    new PatientService.NewPatient(
                            Json.string(body, "name"),
                            Json.string(body, "contactNumber"),
                            Json.string(body, "address"),
                            Json.string(body, "email"),
                            Json.string(body, "dob")));
            // The duplicate warning travels with the record rather than replacing it:
            // two people can share a telephone number, so this is advice, not a refusal.
            writeJson(response, HttpServletResponse.SC_CREATED, created);
        });
    }
}
