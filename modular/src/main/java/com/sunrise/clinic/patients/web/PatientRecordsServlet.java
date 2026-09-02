package com.sunrise.clinic.patients.web;

import com.sunrise.clinic.patients.service.PatientService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The front desk's patient register: search existing records, and register someone
 * who walked in or telephoned.
 *
 * <p>A patient registered here has no portal account, which is the ordinary case at
 * a clinic - the record exists because they were treated, not because they signed
 * up. Their {@code userUid} stays null until they register themselves.</p>
 *
 * <p>The register is paginated at {@value #PAGE_SIZE} rows a page. The page number
 * travels in the query string alongside the search term, and the pager that fallows
 * the table preserves both, so any combination of search and page is a stable,
 * refreshable address (the same GET contract the search already follows).</p>
 */
public class PatientRecordsServlet extends PageServlet {

    /** Rows a screen of the register shows. Small enough to keep a mobile page
     *  light, large enough that the front desk does not click through a lot. */
    static final int PAGE_SIZE = 10;

    /** Page-number buttons either side of the current one, so a long register does
     *  not draw a hundred links. */
    private static final int WINDOW = 2;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String query = field(request, "q");
            PatientService.PatientPage result = app().patientService().searchPage(
                    currentUser(request), query, parsePage(field(request, "page")), PAGE_SIZE);

            request.setAttribute("q", query);
            request.setAttribute("patients", result.patients());
            request.setAttribute("total", result.total());
            request.setAttribute("page", result.page());
            request.setAttribute("pageSize", result.pageSize());
            request.setAttribute("totalPages", result.totalPages());
            request.setAttribute("firstOnPage", result.firstOnPage());
            request.setAttribute("lastOnPage", result.lastOnPage());
            request.setAttribute("prevPage", result.page() > 1 ? result.page() - 1 : null);
            request.setAttribute("nextPage",
                    result.page() < result.totalPages() ? result.page() + 1 : null);
            request.setAttribute("pages",
                    window(result.page(), result.totalPages(), WINDOW));

            // Set by the redirect after a successful registration. Resolved back into
            // the record rather than trusted as text, so the confirmation names the
            // patient and the duplicate warning is still right after a refresh.
            String registeredId = field(request, "registered");
            if (registeredId != null) {
                request.setAttribute("registered",
                        app().patientService().findById(currentUser(request), registeredId));
                request.setAttribute("possibleDuplicates",
                        app().patientService().possibleDuplicatesOf(currentUser(request), registeredId));
            }
            render(request, response, "patients/register");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            PatientService.Registration created = app().patientService().register(
                    currentUser(request),
                    new PatientService.NewPatient(
                            requiredField(request, "name", "Patient name"),
                            requiredField(request, "contactNumber", "Contact number"),
                            field(request, "address"),
                            field(request, "email"),
                            field(request, "dob")));

            // Redirect after post, so a refresh does not register the patient twice.
            // Only the id travels in the query string; the screen resolves it.
            redirect(request, response, "/reception/patients?registered=" + created.patient().id());
        });
    }

    /** A typed page number: an absent or unreadable value means the first page. */
    private static int parsePage(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** The page-number buttons to draw, centred on {@code current} and clamped to
     *  the ends of the register so the hand-built pager never shows a hole. */
    private static List<Integer> window(int current, int totalPages, int radius) {
        int from = Math.max(1, current - radius);
        int to = Math.min(totalPages, current + radius);
        return IntStream.rangeClosed(from, to).boxed().toList();
    }
}