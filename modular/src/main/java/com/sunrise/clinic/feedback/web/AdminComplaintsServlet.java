package com.sunrise.clinic.feedback.web;

import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The administrator's complaints screen - FR-ADM-50 to FR-ADM-58.
 *
 * <p>Open complaints first, so one submitted last week and untouched sits above one resolved
 * this morning. Closing requires a written resolution, and the patient's own account of what
 * happened cannot be edited from here or anywhere else.</p>
 */
public class AdminComplaintsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String action = requiredField(request, "action", "Action");
            String id = requiredField(request, "complaintId", "Complaint");
            switch (action) {
                case "review" -> app().complaintService().beginReview(currentUser(request), id);
                case "resolve" -> app().complaintService().close(currentUser(request), id,
                        ComplaintStatus.RESOLVED, requiredField(request, "resolution", "Resolution"));
                case "dismiss" -> app().complaintService().close(currentUser(request), id,
                        ComplaintStatus.DISMISSED, requiredField(request, "resolution", "Reason"));
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
            redirect(request, response, "/admin/complaints?done=" + id);
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String statusFilter = field(request, "status");
        ComplaintStatus status = null;
        if (statusFilter != null) {
            try {
                status = ComplaintStatus.valueOf(statusFilter);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown state: " + statusFilter);
            }
        }
        request.setAttribute("complaints", app().complaintService().search(currentUser(request),
                status, field(request, "dentistId"),
                dateField(request, "from", null), dateField(request, "to", null)));
        request.setAttribute("statuses", ComplaintStatus.values());
        request.setAttribute("status", statusFilter);
        request.setAttribute("dentists",
                app().referenceService().activeDentists(currentUser(request)));
        request.setAttribute("dentistId", field(request, "dentistId"));
        request.setAttribute("done", field(request, "done"));
        render(request, response, "feedback/admin-complaints");
    }
}
