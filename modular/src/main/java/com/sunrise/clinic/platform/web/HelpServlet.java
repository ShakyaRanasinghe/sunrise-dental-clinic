package com.sunrise.clinic.platform.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The help pages — how to use the system, and how to reach the clinic.
 *
 * <p>Public, because someone who cannot sign in is exactly the person most likely
 * to need it. GAP-FTB-12: besides the shared page at {@code /help}, each role has
 * its own page under {@code /help/<role>} describing how <em>that</em> role uses the
 * system. They live under the public {@code /help} prefix (not under the role
 * prefixes) so a sign-in or sign-up screen — itself anonymous — can link to its
 * own help. None of them is advertised on any public page; each is reached from
 * its role's own sign-in screen or dashboard (GAP-PAT-14, FR-ADM-03).</p>
 */
public class HelpServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("clinicPhone", app().clinicIdentity().get("clinic.phone"));
        request.setAttribute("clinicEmail", app().clinicIdentity().get("clinic.email"));
        render(request, response, viewFor(request));
    }

    /**
     * The view for this help address. {@code /help} is the shared public page;
     * {@code /help/patient}, {@code /help/reception}, {@code /help/dentist} and
     * {@code /help/admin} are the role-specific pages. Anything else under
     * {@code /help/} falls back to the shared page rather than a 404, since a
     * help address should never be a dead end.
     */
    static String viewFor(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            return "shared/help";
        }
        return switch (path) {
            case "/patient" -> "shared/help-patient";
            case "/reception" -> "shared/help-reception";
            case "/dentist" -> "shared/help-dentist";
            case "/admin" -> "shared/help-admin";
            default -> "shared/help";
        };
    }
}
