package com.sunrise.clinic.appointments.web;

import com.sunrise.clinic.appointments.domain.AppointmentDetailResponse;
import com.sunrise.clinic.feedback.domain.ReviewResponse;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The patient's own dashboard: their appointments, and the way to book another.
 *
 * <p>Replaces the step-2 stub at {@code /patient/home}.</p>
 */
public class PatientHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            // No id from the request: a patient's own appointments are resolved from
            // their account, so there is no parameter to tamper with.
            var appointments = app().appointmentService().forSelfDetail(currentUser(request));
            request.setAttribute("appointments", appointments);
            request.setAttribute("booked", field(request, "booked"));
            request.setAttribute("cancelled", field(request, "cancelled"));
            request.setAttribute("rated", field(request, "rated"));

            // FR-PAT-70: load existing reviews so the JSP can show filled stars.
            Map<String, ReviewResponse> existingReviews = app().reviewService()
                    .own(currentUser(request)).stream()
                    .collect(Collectors.toMap(ReviewResponse::appointmentNo, r -> r));
            request.setAttribute("existingReviews", existingReviews);

            // GAP-FTB-02: load the bill for each BILLED visit so the dashboard can open a
            // receipt sub-window. Only the owning patient's bills are read (BillingService
            // gates it), and only for BILLED appointments which always have a bill.
            java.util.Map<String, Object> bills = new java.util.HashMap<>();
            for (var a : appointments) {
                if ("BILLED".equals(a.status().name())) {
                    try {
                        bills.put(a.appointmentNo(),
                                app().billingService().forAppointment(currentUser(request), a.appointmentNo()));
                    } catch (Exception ignored) {
                        // A bill could not be read for this visit; the receipt link is simply
                        // not offered. Never fail the whole dashboard because of one row.
                    }
                }
            }
            request.setAttribute("receipts", bills);

            render(request, response, "appointments/patient-home");
        });
    }

    /** Cancelling or rating from the dashboard. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String action = field(request, "action");
            if ("rate".equals(action)) {
                String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
                int rating = Integer.parseInt(requiredField(request, "rating", "Rating"));
                String comment = field(request, "comment");
                app().reviewService().rate(currentUser(request), appointmentNo, rating, comment);
                redirect(request, response, "/patient/home?rated=" + appointmentNo);
            } else {
                String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
                app().appointmentService().cancel(currentUser(request), appointmentNo);
                redirect(request, response, "/patient/home?cancelled=" + appointmentNo);
            }
        });
    }
}
