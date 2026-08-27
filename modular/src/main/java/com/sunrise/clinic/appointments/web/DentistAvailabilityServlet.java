package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.platform.web.PageServlet;
import com.sunrise.clinic.scheduling.domain.SlotStatus;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dentist's own published availability: what time windows reception created,
 * how many slots remain open, how many are booked.
 *
 * <p>Reception publishes these windows (ASM-06) but the dentist previously had
 * no screen showing them — they could only see already-booked appointments.
 * FR-DEN-18, GAP-DEN-05.</p>
 */
public class DentistAvailabilityServlet extends PageServlet {

    /** A session with its slot counts, ready for the JSP. */
    public record SessionInfo(LocalDate date, LocalTime startTime, LocalTime endTime,
                              int total, int open, int booked) {}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String dentistId = app().clinicAccess()
                    .dentistFor(currentUser(request))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No dentist record for this account"))
                    .getId();
            LocalDate today = LocalDate.now();

            var sessions = app().slotService().publishedFor(dentistId);

            Map<LocalDate, List<SessionInfo>> byDate = new LinkedHashMap<>();
            for (var session : sessions) {
                if (session.getDate().isBefore(today)) {
                    continue;
                }
                var slots = app().slotService().allSlots(dentistId, session.getDate());
                int open = (int) slots.stream()
                        .filter(s -> s.status() == SlotStatus.OPEN).count();
                byDate.computeIfAbsent(session.getDate(), k -> new java.util.ArrayList<>())
                        .add(new SessionInfo(
                                session.getDate(),
                                session.getStartTime(),
                                session.getEndTime(),
                                slots.size(),
                                open,
                                slots.size() - open));
            }

            request.setAttribute("byDate", byDate);
            request.setAttribute("hasAvailability", !byDate.isEmpty());
            render(request, response, "appointments/dentist-availability");
        });
    }
}
