package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.DentistSession;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Publishing a dentist's availability.
 *
 * <p>The receptionist enters a window — "Dr. Silva, Monday, 16:00 to 18:00, in
 * half-hour slots" — and the {@code SlotService} explodes it into the individual
 * bookable slots patients then choose from. One form submission can create a dozen
 * rows, which is why the slot generation lives in the service rather than here.</p>
 */
public class AvailabilityPageServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);

            LocalDate date = dateField(request, "date", LocalDate.now());
            request.setAttribute("dentists", app().dentists().findActive());
            request.setAttribute("date", date);
            request.setAttribute("sessions", app().sessions().findByDate(date));
            render(request, response, "reception/availability");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.RECEPTIONIST, Role.ADMIN);

            String dentistId = requiredField(request, "dentistId", "A dentist");
            LocalDate date = dateField(request, "date", null);
            if (date == null) {
                throw new IllegalArgumentException("A date is required.");
            }
            LocalTime start = time(request, "startTime", "Start time");
            LocalTime end = time(request, "endTime", "End time");
            int slotMinutes = slotMinutes(request);

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("The end time must be after the start time.");
            }
            if (date.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Availability cannot be published for a past date.");
            }

            DentistSession session = app().slotService()
                    .publishSession(dentistId, date, start, end, slotMinutes, user.uid());

            int created = app().slots().findBySessionId(session.getId()).size();
            redirect(request, response,
                    "/reception/availability?date=" + date + "&published=" + created);
        });
    }

    private LocalTime time(HttpServletRequest request, String name, String label) {
        String raw = requiredField(request, name, label);
        try {
            return LocalTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " must be a time such as 16:00.");
        }
    }

    private int slotMinutes(HttpServletRequest request) {
        String raw = field(request, "slotMinutes");
        if (raw == null) {
            return 30;
        }
        try {
            int minutes = Integer.parseInt(raw);
            if (minutes <= 0 || minutes > 240) {
                throw new IllegalArgumentException(
                        "Slot length must be between 1 and 240 minutes.");
            }
            return minutes;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Slot length must be a whole number of minutes.");
        }
    }
}
