package com.sunrise.clinic.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The help page — how to use the system, and how to reach the clinic.
 *
 * <p>Public, because someone who cannot sign in is exactly the person most likely
 * to need it.</p>
 */
public class HelpServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("clinicPhone", app().config().get("clinic.phone", "+94 11 234 5678"));
        request.setAttribute("clinicEmail",
                app().config().get("clinic.email", "hello@sunrisedental.lk"));
        render(request, response, "help");
    }
}
