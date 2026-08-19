package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The front door. Sends a signed-in visitor to the page for their role, and
 * everyone else to the login screen.
 */
public class HomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClinicPrincipal user = currentUser(request);
        redirect(request, response,
                user == null ? "/login" : AuthenticationFilter.homeFor(user.role()));
    }
}
