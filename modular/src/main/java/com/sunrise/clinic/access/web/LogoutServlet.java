package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Ends the session and returns the visitor to their own front door.
 *
 * <p>GAP-ADM-14: staff land back on the central staff portal ({@code /staff}),
 * where their three doors live; patients return to the public home page, as
 * before. The role is read before the session is cleared.</p>
 */
public class LogoutServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClinicPrincipal outgoing = AuthenticationFilter.currentPrincipal(request);
        AuthenticationFilter.clearSession(request);
        boolean staff = outgoing != null && outgoing.getRole() != Role.PATIENT;
        redirect(request, response, staff ? "/staff" : "/");
    }

    /** Accept POST too, so the header's sign-out control can be a form. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }
}
