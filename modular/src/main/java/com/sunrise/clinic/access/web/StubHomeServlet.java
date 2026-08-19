package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * TEMPORARY — delete at step 4.
 *
 * <p>Stands in for the four role landing pages so that sign-in has somewhere to
 * go. Without it {@code HomeServlet} would redirect a signed-in receptionist to
 * {@code /reception/home}, which the {@code appointments} module owns and which
 * does not exist until step 4 — so the first four steps of the migration would
 * have produced a login page leading to a 404.</p>
 *
 * <p>Mapped four times in {@code web.xml}, to the four home paths. Step 4
 * replaces three of those mappings with the real dashboards and step 7 replaces
 * the fourth, at which point this class and its view are deleted.</p>
 */
public class StubHomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        render(request, response, "shared/stub-home");
    }
}
