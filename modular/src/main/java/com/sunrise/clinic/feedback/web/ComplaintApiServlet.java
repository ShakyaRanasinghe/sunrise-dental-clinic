package com.sunrise.clinic.feedback.web;

import com.sunrise.clinic.feedback.domain.ComplaintCategory;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Complaints as JSON.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET  /api/complaints}</td><td>the caller's own, or the administrator's list</td></tr>
 *   <tr><td>{@code POST /api/complaints}</td><td>raise one</td></tr>
 *   <tr><td>{@code POST /api/complaints/{id}/review}</td><td>pick it up</td></tr>
 *   <tr><td>{@code POST /api/complaints/{id}/resolve}</td><td>close it</td></tr>
 *   <tr><td>{@code POST /api/complaints/{id}/dismiss}</td><td>close it without action</td></tr>
 * </table>
 *
 * <p>There is no route here a dentist can call successfully. That is not enforced by this
 * class - {@code ComplaintService} refuses them - and the difference matters: a second
 * servlet added later gets the same refusal without anybody remembering to write it.</p>
 */
public class ComplaintApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            if (!pathParts(request).isEmpty()) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            // A patient asks for theirs; an administrator asks for everyone's. One route,
            // because "what complaints are there" is one question from two positions.
            if (currentUser(request) != null
                    && currentUser(request).role() == com.sunrise.clinic.access.domain.Role.PATIENT) {
                writeJson(response, app().complaintService().own(currentUser(request)));
                return;
            }
            writeJson(response, app().complaintService().search(currentUser(request),
                    status(request.getParameter("status")),
                    request.getParameter("dentistId"), null, null));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                Map<String, Object> body = readBody(request);
                writeJson(response, HttpServletResponse.SC_CREATED,
                        app().complaintService().raise(currentUser(request),
                                Json.string(body, "dentistId"),
                                Json.string(body, "appointmentNo"),
                                category(Json.string(body, "category")),
                                Json.string(body, "detail")));
                return;
            }
            if (path.size() != 2) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            String id = path.get(0);
            switch (path.get(1)) {
                case "review" -> writeJson(response,
                        app().complaintService().beginReview(currentUser(request), id));
                case "resolve" -> writeJson(response, app().complaintService().close(
                        currentUser(request), id, ComplaintStatus.RESOLVED,
                        Json.string(readBody(request), "resolution")));
                case "dismiss" -> writeJson(response, app().complaintService().close(
                        currentUser(request), id, ComplaintStatus.DISMISSED,
                        Json.string(readBody(request), "resolution")));
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    private static ComplaintCategory category(String raw) {
        try {
            return ComplaintCategory.valueOf(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("category must be one of CONDUCT, "
                    + "CLINICAL_CONCERN, WAIT_TIME, BILLING, OTHER");
        }
    }

    private static ComplaintStatus status(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ComplaintStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown state: " + raw);
        }
    }
}
