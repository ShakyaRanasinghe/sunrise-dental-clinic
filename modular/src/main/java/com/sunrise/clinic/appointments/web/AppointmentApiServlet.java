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
 *   <tr><td>{@code POST /api/appointments/{no}/bill}</td><td>issue the bill</td></tr>
 *   <tr><td>{@code GET  /api/appointments/{no}/bill}</td><td>fetch it</td></tr>
 * </table>
 *
 * <p>The billing routes live under this prefix because a prefix mapping can only be served
 * by one servlet. They delegate to {@code BillingService}, which owns the rules about who
 * may read a bill and whether one may be issued at all.</p>
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
                case "complete" -> {
                    Map<String, Object> body = readBody(request);
                    writeJson(response, app().appointmentService().complete(
                            currentUser(request), appointmentNo,
                            Json.string(body, "diagnosis"), optionalPrice(body)));
                }
                case "bill" -> writeJson(response, HttpServletResponse.SC_CREATED,
                        app().billingService().issue(currentUser(request), appointmentNo));
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
                writeJson(response, app().billingService()
                        .forAppointment(currentUser(request), path.get(0)));
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

    /** GAP-DEN-13: optional ad-hoc price for treatment-less work; blank absorbs. */
    private static java.math.BigDecimal optionalPrice(Map<String, Object> body) {
        String raw = Json.string(body, "customPrice");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a number, like 4500.00");
        }
    }
}
