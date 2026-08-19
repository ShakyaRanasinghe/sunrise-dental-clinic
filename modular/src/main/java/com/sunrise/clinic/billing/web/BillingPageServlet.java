package com.sunrise.clinic.billing.web;

import com.sunrise.clinic.billing.domain.BillResponse;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The front desk's billing screen: the day's completed appointments, and what has already
 * been billed.
 *
 * <p>Shows both states rather than only the unbilled ones, because "have I billed this
 * yet?" is the question the desk actually has - and hiding the billed ones would make the
 * answer "it is not in the list", which is the same answer as "it does not exist".</p>
 */
public class BillingPageServlet extends PageServlet {

    /** What the screen needs about one appointment: it, and its bill if there is one. */
    public record Billable(com.sunrise.clinic.appointments.domain.AppointmentResponse appointment,
                           BillResponse bill) {

        public boolean isBilled() {
            return bill != null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            LocalDate date = dateField(request, "date", LocalDate.now());
            request.setAttribute("date", date);
            request.setAttribute("billables", billablesOn(request, date));
            request.setAttribute("issued", field(request, "issued"));
            render(request, response, "billing/billing");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String appointmentNo = requiredField(request, "appointmentNo", "Appointment number");
            LocalDate date = dateField(request, "date", LocalDate.now());
            BillResponse issued = app().billingService().issue(currentUser(request), appointmentNo);
            // Straight to the receipt: issuing a bill and handing it over are one action at
            // the desk, and a redirect means a refresh cannot bill twice.
            redirect(request, response, "/reception/receipt?appointmentNo=" + issued.appointmentNo());
        });
    }

    private List<Billable> billablesOn(HttpServletRequest request, LocalDate date) {
        List<Billable> billables = new ArrayList<>();
        for (var appointment : app().appointmentService().onDate(currentUser(request), date)) {
            switch (appointment.status()) {
                case COMPLETED -> billables.add(new Billable(appointment, null));
                case BILLED -> billables.add(new Billable(appointment,
                        app().billingService().forAppointment(currentUser(request),
                                appointment.appointmentNo())));
                // CONFIRMED has not been treated yet and CANCELLED never will be. Neither
                // is billable, and listing them would invite the desk to try.
                default -> { }
            }
        }
        return billables;
    }
}
