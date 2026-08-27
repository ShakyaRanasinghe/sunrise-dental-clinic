package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.platform.web.PageServlet;
import com.sunrise.clinic.scheduling.domain.DentistResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The front door — and, for anyone not signed in, the clinic's public face.
 *
 * <p>A first-time visitor used to land on the role chooser with nothing about the
 * clinic itself: no services, no dentists, no address. That page answers a question
 * only an existing user asks ("which door am I?"); a newcomer asks "what is this
 * place?" (scenario gap 1 in docs/scenarios/patient-online-journey.md). So an
 * anonymous visitor now gets a real landing page — services and dentists from the
 * live catalogue, contact details from configuration — with sign-in and registration
 * in the navigation bar. A signed-in visitor still goes straight to their role's
 * home; they have no business on a brochure.</p>
 */
public class HomeServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        ClinicPrincipal user = currentUser(request);
        if (user != null) {
            redirect(request, response, AuthenticationFilter.homeFor(user.role()));
            return;
        }
        var dentists = app().referenceService().directoryDentists();
        request.setAttribute("dentists", dentists);
        request.setAttribute("treatments", app().referenceService().directoryTreatments());
        request.setAttribute("clinicName", app().clinicIdentity().get("clinic.name"));
        request.setAttribute("clinicPhone", app().clinicIdentity().get("clinic.phone"));
        request.setAttribute("clinicEmail", app().clinicIdentity().get("clinic.email"));
        request.setAttribute("clinicAddress", app().clinicIdentity().get("clinic.address"));

        // FR-RVW-11: aggregate ratings per dentist for the public landing page.
        Map<String, RatingSummary> ratings = dentists.stream()
                .collect(Collectors.toMap(
                        DentistResponse::id,
                        d -> app().reviewService().summaryFor(d.id())));
        request.setAttribute("ratings", ratings);

        render(request, response, "access/home");
    }
}
