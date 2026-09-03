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
            var dentist = app().clinicAccess()
                    .dentistFor(currentUser(request))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No dentist record for this account"));
            LocalDate today = LocalDate.now();

            var sessions = app().slotService().publishedFor(dentist.getId());

            Map<LocalDate, List<SessionInfo>> byDate = new LinkedHashMap<>();
            for (var session : sessions) {
                if (session.getDate().isBefore(today)) {
                    continue;
                }
                var slots = app().slotService().allSlots(dentist.getId(), session.getDate());
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

            request.setAttribute("dentist", dentist);
            request.setAttribute("phoneSaved", field(request, "saved"));
            // GAP-FTB-07: the dentist's own treatment list, offered + toggleable.
            request.setAttribute("toggles",
                    app().referenceService().dentistTreatmentToggles(dentist.getId()));
            request.setAttribute("byDate", byDate);
            request.setAttribute("hasAvailability", !byDate.isEmpty());
            render(request, response, "appointments/dentist-availability");
        });
    }

    /** Save the dentist's own public phone number (GAP-FTB-04) or toggle a treatment
     * they offer (GAP-FTB-07). */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            var dentist = app().clinicAccess()
                    .dentistFor(currentUser(request))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No dentist record for this account"));
            String action = field(request, "action");
            if ("phone".equals(action)) {
                app().referenceService().updateOwnPhone(
                        currentUser(request), currentUser(request).uid(), field(request, "phone"));
                redirect(request, response, "/dentist/availability?saved=yes");
            } else if ("toggle".equals(action)) {
                String treatmentId = field(request, "treatmentId");
                boolean offered = "on".equals(field(request, "offered"));
                app().referenceService().setTreatmentOffered(
                        currentUser(request), dentist.getId(), treatmentId, offered);
                redirect(request, response, "/dentist/availability#treatments");
            } else {
                throw new IllegalArgumentException("Unknown action.");
            }
        });
    }
}
