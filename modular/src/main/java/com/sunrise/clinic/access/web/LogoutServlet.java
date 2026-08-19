package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Ends the session and returns the visitor to the login page. */
public class LogoutServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthenticationFilter.clearSession(request);
        redirect(request, response, "/login?signedOut=1");
    }

    /** Accept POST too, so the header's sign-out control can be a form. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }
}
