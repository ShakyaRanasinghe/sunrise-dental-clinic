package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Billing at the front desk.
 *
 * <p>{@code GET} looks an appointment up and shows either the bill already issued
 * for it or a button to produce one; {@code POST} generates it. The arithmetic is
 * not done here — {@code BillingService} delegates to the two Strategy objects that
 * own the pricing and commission rules — so this servlet only decides who may press
 * the button and what to show afterwards.</p>
 *
 * <p>The receipt shown to the patient deliberately omits the three-way revenue
 * split. Those figures exist on the {@code Bill}, but they are the clinic's internal
 * business and reach only the Administrator's reports.</p>
 */
public class BillingPageServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.RECEPTIONIST, Role.ADMIN);

            String appointmentNo = field(request, "appointmentNo");
            if (appointmentNo != null) {
                Appointment appointment = app().appointmentService().findByNo(appointmentNo);
                Optional<Bill> existing = app().bills().findByAppointmentNo(appointmentNo);

                request.setAttribute("appointment", appointment);
                request.setAttribute("bill", existing.orElse(null));
                request.setAttribute("patient",
                        app().patients().findById(appointment.getPatientId()).orElse(null));
                request.setAttribute("dentist",
                        app().dentists().findById(appointment.getDentistId()).orElse(null));
                request.setAttribute("treatment", appointment.getTreatmentId() == null ? null
                        : app().treatments().findById(appointment.getTreatmentId()).orElse(null));
            }

            request.setAttribute("appointmentNo", appointmentNo);
            render(request, response, "reception/billing");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicPrincipal user = currentUser(request);
            AccessControl.require(user, Role.RECEPTIONIST, Role.ADMIN);

            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            if (app().bills().findByAppointmentNo(appointmentNo).isPresent()) {
                throw new IllegalArgumentException(
                        "A bill has already been issued for " + appointmentNo + ".");
            }

            app().billingService().generateBill(appointmentNo, user.uid());
            redirect(request, response,
                    "/reception/billing?appointmentNo=" + appointmentNo + "&issued=1");
        });
    }
}
