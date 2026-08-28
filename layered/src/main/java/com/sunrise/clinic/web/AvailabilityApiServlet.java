package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.DentistSession;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.dto.SlotResponse;
import com.sunrise.clinic.json.Json;
import com.sunrise.clinic.security.ClinicPrincipal;
import com.sunrise.clinic.security.AccessControl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Availability browsing and publishing.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code GET  /api/availability?dentistId&date}</td><td>open slots for a day</td></tr>
 *   <tr><td>{@code GET  /api/availability/week?from&to}</td><td>open slots across a range</td></tr>
 *   <tr><td>{@code POST /api/sessions}</td><td>publish a dentist's availability window</td></tr>
 * </table>
 *
 * <p>{@code POST /api/sessions} is mapped to this servlet as well, because
 * publishing a session and reading the slots it produces are two halves of the same
 * feature.</p>
 */
public class AvailabilityApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                day(request, response);
            } else if (path.size() == 1 && "week".equals(path.get(0))) {
                week(request, response);
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> publishSession(request, response));
    }

    private void day(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dentistId = requiredParam(request, "dentistId");
        LocalDate date = requiredDate(request, "date");
        List<SlotResponse> slots = app().slotService().openSlots(dentistId, date)
                .stream().map(app().mapper()::toSlotResponse).toList();
        writeJson(response, slots);
    }

    private void week(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LocalDate from = requiredDate(request, "from");
        LocalDate to = requiredDate(request, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        List<SlotResponse> slots = app().slotService().openSlotsBetween(from, to)
                .stream().map(app().mapper()::toSlotResponse).toList();
        writeJson(response, slots);
    }

    private void publishSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ClinicPrincipal user = currentUser(request);
        AccessControl.require(user, Role.RECEPTIONIST, Role.ADMIN);

        Map<String, Object> body = readBody(request);
        String dentistId = requiredField(body, "dentistId");
        LocalDate date = parseDate(requiredField(body, "date"), "date");
        LocalTime start = parseTime(requiredField(body, "startTime"), "startTime");
        LocalTime end = parseTime(requiredField(body, "endTime"), "endTime");
        int slotMinutes = Json.integer(body, "slotMinutes", 30);

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (slotMinutes <= 0) {
            throw new IllegalArgumentException("slotMinutes must be greater than zero");
        }

        DentistSession session = app().slotService()
                .publishSession(dentistId, date, start, end, slotMinutes, user.uid());
        List<SlotResponse> slots = app().slotService().openSlots(dentistId, date)
                .stream().map(app().mapper()::toSlotResponse).toList();

        writeJson(response, HttpServletResponse.SC_CREATED,
                Map.of("sessionId", session.getId(), "slots", slots));
    }

    private static String requiredField(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static LocalDate parseDate(String raw, String field) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be a date in yyyy-MM-dd format");
        }
    }

    private static LocalTime parseTime(String raw, String field) {
        try {
            return LocalTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be a time in HH:mm format");
        }
    }
}
