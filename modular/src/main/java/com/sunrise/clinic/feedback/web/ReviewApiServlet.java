package com.sunrise.clinic.feedback.web;

import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Reviews as JSON.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET  /api/reviews}</td><td>a patient's own, or a dentist's own aggregate</td></tr>
 *   <tr><td>{@code GET  /api/reviews/{dentistId}}</td><td>individual reviews - administrator only</td></tr>
 *   <tr><td>{@code POST /api/reviews}</td><td>rate a visit, or change an existing rating</td></tr>
 * </table>
 *
 * <p>{@code GET /api/reviews} deliberately returns <b>different types</b> to different
 * callers: a list of reviews to the patient who wrote them, and a {@code RatingSummary} to a
 * dentist. That is the confidentiality rule expressed as a type rather than as a filter -
 * there is no code path that can hand a dentist a comment, because the object it returns has
 * no field for one.</p>
 */
public class ReviewApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.size() == 1) {
                // Administrator only: individual reviews, with comments.
                writeJson(response, app().reviewService()
                        .forDentist(currentUser(request), path.get(0)));
                return;
            }
            if (!path.isEmpty()) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            if (currentUser(request) != null && currentUser(request).role() == Role.DENTIST) {
                // A summary, never a review. See the class comment.
                writeJson(response, app().reviewService().ownSummary(currentUser(request)));
                return;
            }
            writeJson(response, app().reviewService().own(currentUser(request)));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            if (!pathParts(request).isEmpty()) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            Map<String, Object> body = readBody(request);
            Object rating = body.get("rating");
            if (!(rating instanceof Number number)) {
                throw new IllegalArgumentException("rating must be a number from 1 to 5");
            }
            writeJson(response, HttpServletResponse.SC_CREATED,
                    app().reviewService().rate(currentUser(request),
                            Json.string(body, "appointmentNo"),
                            number.intValue(),
                            Json.string(body, "comment")));
        });
    }
}
