package com.sunrise.clinic.reception.web;

import com.sunrise.clinic.platform.service.ClinicIdentityService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Reception screen for editing the clinic phone number only.
 *
 * <p>The phone number changes more often than name, email or address —
 * a new line, a temporary redirection — so the front desk updates it
 * directly rather than asking the administrator. Only this one field is
 * exposed; email, address and name require {@link Action#MANAGE_CLINIC_SETTINGS}
 * and belong on the admin screen.</p>
 */
public class ReceptionPhoneServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            request.setAttribute("clinicPhone",
                    app().clinicIdentity().get("clinic.phone"));
            render(request, response, "reception/phone");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String phone = requiredField(request, "clinic.phone", "Phone number");
            app().clinicIdentity().update(currentUser(request), "clinic.phone", phone);
            redirect(request, response, "/reception/phone?saved=1");
        });
    }
}
