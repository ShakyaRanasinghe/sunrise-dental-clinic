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
 * <p>Medical notes hang off {@code /api/patients/{id}/notes} - all four methods. Who may
 * read or write one is decided by {@code PatientNoteService}, not here: a receptionist
 * calling any of them receives nothing rather than a redacted note.</p>
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
            } else if (path.size() == 2 && "notes".equals(path.get(1))) {
                writeJson(response, app().patientNoteService()
                        .forPatient(currentUser(request), path.get(0)));
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.size() == 2 && "notes".equals(path.get(1))) {
                // The patient id in the path is not trusted: the service resolves the
                // caller's own patient record and refuses anything else.
                Map<String, Object> note = readBody(request);
                writeJson(response, HttpServletResponse.SC_CREATED,
                        app().patientNoteService().declare(currentUser(request),
                                noteCategory(note), Json.string(note, "detail"),
                                Boolean.TRUE.equals(note.get("critical"))));
                return;
            }
            if (!path.isEmpty()) {
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

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.size() != 3 || !"notes".equals(path.get(1))) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            Map<String, Object> note = readBody(request);
            writeJson(response, app().patientNoteService().amend(currentUser(request), path.get(2),
                    noteCategory(note), Json.string(note, "detail"),
                    Boolean.TRUE.equals(note.get("critical"))));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.size() != 3 || !"notes".equals(path.get(1))) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            app().patientNoteService().withdraw(currentUser(request), path.get(2));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        });
    }

    private static com.sunrise.clinic.patients.domain.NoteCategory noteCategory(
            Map<String, Object> body) {
        String raw = Json.string(body, "category");
        try {
            return com.sunrise.clinic.patients.domain.NoteCategory.valueOf(
                    raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "category must be one of ALLERGY, MEDICATION, CONDITION, OTHER");
        }
    }
}
