package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.platform.service.ClinicIdentityService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Administrator screen for clinic identity — name, phone, email, address.
 *
 * <p>These values were previously locked in {@code clinic.properties} and
 * only changeable by editing the file and redeploying (GAP-ADM-06). They
 * are now stored in the {@code clinic_setting} table and editable at
 * runtime. Every field requires {@link Action#MANAGE_CLINIC_SETTINGS}.</p>
 */
public class ClinicIdentityServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response, null));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicIdentityService svc = app().clinicIdentity();
            String[] keys = {"clinic.name", "clinic.phone", "clinic.email", "clinic.address"};
            for (String key : keys) {
                String value = request.getParameter(key);
                if (value != null) {
                    svc.update(currentUser(request), key, value);
                }
            }
            redirect(request, response, "/admin/clinic?saved=1");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response, String notice)
            throws ServletException, IOException {
        Map<String, String> values = app().clinicIdentity().getAll();
        request.setAttribute("values", values);
        request.setAttribute("notice", notice);
        render(request, response, "reporting/clinic-identity");
    }
}
