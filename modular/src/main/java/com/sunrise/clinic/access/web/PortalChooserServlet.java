package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The four doors at {@code /login}.
 *
 * <p>Lists three of them. {@code /login/admin} works and is deliberately not
 * linked, because no member of the public has business there (FR-ADM-03) — it is
 * reachable by anyone who is told the address, which is the point.</p>
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
