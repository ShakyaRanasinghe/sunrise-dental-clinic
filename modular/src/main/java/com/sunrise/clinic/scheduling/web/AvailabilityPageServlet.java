package com.sunrise.clinic.scheduling.web;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.web.PageServlet;
import com.sunrise.clinic.scheduling.service.SlotService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Publish availability: pick a dentist, a date and the hours, and the bookable times
 * are generated.
 *
 * <p>The screen shows what is already published for the dentist and date being looked
 * at, because the commonest mistake at the desk is publishing a window that overlaps
 * one already there - which the service now refuses, so the screen should show why
 * before the refusal happens.</p>
 */
public class AvailabilityPageServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Action.PUBLISH_AVAILABILITY);
            show(request, response, field(request, "dentistId"),
                    dateField(request, "date", LocalDate.now()), null, null, null);
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String dentistId = requiredField(request, "dentistId", "Dentist");
            LocalDate date = dateField(request, "date", null);
            if (date == null) {
                throw new IllegalArgumentException("Please choose a date.");
            }
            try {
                SlotService.Published result = app().slotService().publishSession(
                        currentUser(request),
                        new SlotService.NewSession(dentistId, date,
                                time(request, "startTime", "From"),
                                time(request, "endTime", "To"),
                                minutes(request)));

                // Rendered rather than redirected, because the warning about an uneven
                // window belongs beside the slots it is about.
                show(request, response, dentistId, date,
                        result.slots().size() + " bookable times published.",
                        result.warning(), null);
            } catch (IllegalArgumentException e) {
                // Conflict (overlap) — stay on the page so the receptionist can see
                // what is already published for this dentist and date.
                show(request, response, dentistId, date, null, null, e.getMessage());
            }
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      String dentistId, LocalDate date, String confirmation, String warning,
                      String error)
            throws ServletException, IOException {
        request.setAttribute("dentists", app().referenceService().activeDentists(currentUser(request)));
        request.setAttribute("dentistId", dentistId);
        request.setAttribute("date", date);
        request.setAttribute("confirmation", confirmation);
        request.setAttribute("warning", warning);
        request.setAttribute("error", error);
        // Always show the full upcoming diary so reception sees what is published
        // before attempting to publish, not just after a conflict.
        request.setAttribute("upcomingSessions",
                app().slotService().upcomingSessions(LocalDate.now()));
        if (dentistId != null && !dentistId.isBlank()) {
            request.setAttribute("published", app().slotService().allSlots(dentistId, date));
            request.setAttribute("dentistName",
                    app().referenceService().requireDentist(dentistId).getName());
        }
        render(request, response, "scheduling/availability");
    }

    private static LocalTime time(HttpServletRequest request, String name, String label) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " must be a time like 09:00.");
        }
    }

    private static int minutes(HttpServletRequest request) {
        String raw = request.getParameter("slotMinutes");
        if (raw == null || raw.isBlank()) {
            return 30;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Appointment length must be a number of minutes.");
        }
    }
}
