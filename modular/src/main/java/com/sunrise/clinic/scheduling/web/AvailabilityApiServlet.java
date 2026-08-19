package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;
import com.sunrise.clinic.scheduling.service.SlotService;

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
 *   <tr><td>{@code POST /api/sessions}</td><td>publish a window</td></tr>
 * </table>
 *
 * <p>{@code /api/sessions} is mapped here too: publishing a window and reading the
 * slots it produces are two halves of one feature, and splitting them across servlets
 * would put the same validation in two places.</p>
 *
 * <p>All four of the validation rules this endpoint was missing now live in
 * {@link SlotService}, not here - a rule enforced in a servlet is a rule the next
 * caller can bypass.</p>
 */
public class AvailabilityApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                writeJson(response, app().slotService().openSlots(
                        requiredParam(request, "dentistId"), requiredDate(request, "date")));
            } else if (path.size() == 1 && "week".equals(path.get(0))) {
                writeJson(response, app().slotService().openSlotsBetween(
                        requiredDate(request, "from"), requiredDate(request, "to")));
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            Map<String, Object> body = readBody(request);
            SlotService.Published published = app().slotService().publishSession(
                    currentUser(request),
                    new SlotService.NewSession(
                            requiredField(body, "dentistId"),
                            parseDate(requiredField(body, "date")),
                            parseTime(requiredField(body, "startTime"), "startTime"),
                            parseTime(requiredField(body, "endTime"), "endTime"),
                            Json.integer(body, "slotMinutes", 30)));

            writeJson(response, HttpServletResponse.SC_CREATED, published);
        });
    }

    private static String requiredField(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date must be a date in yyyy-MM-dd format");
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
