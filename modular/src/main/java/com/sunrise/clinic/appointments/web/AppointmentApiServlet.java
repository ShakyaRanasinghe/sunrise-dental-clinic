package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything under {@code /api/appointments}.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code POST /api/appointments}</td><td>book</td></tr>
 *   <tr><td>{@code GET  /api/appointments?date=}</td><td>a day's list, or the caller's own</td></tr>
 *   <tr><td>{@code GET  /api/appointments/{no}}</td><td>one, with the diagnosis if permitted</td></tr>
 *   <tr><td>{@code POST /api/appointments/{no}/cancel}</td><td>cancel</td></tr>
 *   <tr><td>{@code POST /api/appointments/{no}/complete}</td><td>record the diagnosis</td></tr>
 * </table>
 *
 * <p>The billing sub-paths - {@code /{no}/bill} - are part of this prefix in the route
 * contract, because a prefix mapping can only be served by one servlet. They arrive with
 * the billing module; until then they are refused rather than silently treated as
 * something else.</p>
 *
 * <p>Every route delegates. This class no longer resolves a patient from an account, and
 * no longer reaches a repository to do it - the previous version did both, twice.</p>
 */
public class AppointmentApiServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                book(request, response);
                return;
            }
            if (path.size() != 2) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            String appointmentNo = path.get(0);
            switch (path.get(1)) {
                case "cancel" -> writeJson(response,
                        app().appointmentService().cancel(currentUser(request), appointmentNo));
                case "complete" -> writeJson(response, app().appointmentService().complete(
                        currentUser(request), appointmentNo,
                        Json.string(readBody(request), "diagnosis")));
                case "bill" -> throw new IllegalArgumentException(
                        "Billing is not available yet.");
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                list(request, response);
            } else if (path.size() == 1) {
                // findDetail returns one of two record types, chosen by ClinicAccess.
                writeJson(response,
                        app().appointmentService().findDetail(currentUser(request), path.get(0)));
            } else if (path.size() == 2 && "bill".equals(path.get(1))) {
                throw new IllegalArgumentException("Billing is not available yet.");
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    private void book(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = readBody(request);
        writeJson(response, HttpServletResponse.SC_CREATED, app().appointmentService().book(
                currentUser(request),
                required(body, "slotId"),
                required(body, "treatmentId"),
                Json.string(body, "patientId")));
    }

    /**
     * {@code ?date=} lists that day for staff; with no parameter a patient gets their own
     * appointments. One route, because "what is booked" is one question asked from two
     * positions - and answering it at all is new.
     */
    private void list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String date = request.getParameter("date");
        if (date == null || date.isBlank()) {
            writeJson(response, app().appointmentService().forSelf(currentUser(request)));
            return;
        }
        writeJson(response, app().appointmentService()
                .onDate(currentUser(request), LocalDate.parse(date)));
    }

    private static String required(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
