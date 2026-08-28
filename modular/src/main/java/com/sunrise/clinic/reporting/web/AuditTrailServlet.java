package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The audit trail - FR-ADM-30 to FR-ADM-33.
 *
 * <p>Read-only, and there is no servlet, route or service method that edits or deletes a
 * record (FR-ADM-31). Filtering by target answers "who changed this appointment, and when"
 * for any appointment number (FR-ADM-32).</p>
 *
 * <p>The trail records <b>that</b> a diagnosis was written, never what it said (FR-ADM-33).
 * That is a property of what {@code AuditEvent} carries - action, actor, target, time - and
 * not of this screen, so no view can leak it.</p>
 */
public class AuditTrailServlet extends PageServlet {

    private static final int PAGE_SIZE = 200;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Action.READ_AUDIT);

            String actorUid = field(request, "actorUid");
            String targetId = field(request, "targetId");
            request.setAttribute("actorUid", actorUid);
            request.setAttribute("targetId", targetId);
            request.setAttribute("from", field(request, "from"));
            request.setAttribute("to", field(request, "to"));
            request.setAttribute("events", app().auditTrail().search(
                    actorUid, targetId,
                    dateField(request, "from", null),
                    dateField(request, "to", null),
                    PAGE_SIZE));
            request.setAttribute("pageSize", PAGE_SIZE);
            render(request, response, "reporting/audit");
        });
    }
}
