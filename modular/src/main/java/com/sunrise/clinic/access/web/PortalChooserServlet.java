package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The doors at {@code /login}.
 *
 * <p>Lists one of them. The patient portal is the only door a member of the
 * public signs straight into; the others — {@code /login/reception},
 * {@code /login/dentist} and {@code /login/admin} — all work and are
 * deliberately not linked, because no member of the public has business there
 * and a patient should not even learn that the staff doors exist (FR-ADM-03,
 * GAP-PAT-14). Each is reachable by anyone who is told the address, which is
 * the point.</p>
 */
public class PortalChooserServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (currentUser(request) != null) {
            redirect(request, response, currentUser(request).policy().homePath());
            return;
        }
        render(request, response, "access/portal-chooser");
    }
}
