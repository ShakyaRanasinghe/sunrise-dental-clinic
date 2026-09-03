package com.sunrise.clinic.access.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The central staff portal — one page that lists the three staff doors and sends
 * each role to its own sign-in screen.
 *
 * <p>The clinic's public home page is a patient-facing brochure and carries the
 * patient door in its navigation. Staff reach their portal by a separate address
 * ({@code /staff}) instead: an administrator, receptionist or dentist should not
 * have to walk through the patient's public page to find their own sign-in. The
 * page deliberately lists only the staff roles — the patient door stays on the
 * public home, so a first-time patient is not offered three logins that can never
 * admit them.</p>
 *
 * <p>The role chooser that originally lived here was dropped because it only offered
 * the patient door; this page is the staff-only equivalent that replaced it.</p>
 */
public class StaffPortalServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "access/staff-portal");
    }
}
