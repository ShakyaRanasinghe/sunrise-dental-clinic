package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * The report as a CSV download - FR-ADM-18.
 *
 * <p>Writes bytes rather than rendering a view, so it does not extend the page pattern's
 * behaviour of forwarding to a JSP. It still uses {@code page(...)} for the error handling:
 * a failure here should show the administrator an error page, not download a file
 * containing a stack trace.</p>
 */
public class CsvExportServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            LocalDate from = dateField(request, "from", app().reportService().defaultFrom());
            LocalDate to = dateField(request, "to", app().reportService().today());
            String csv = app().reportService().asCsv(currentUser(request), from, to);

            response.setContentType("text/csv;charset=UTF-8");
            // A filename carrying the period, so a folder of exports stays readable.
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"sunrise-report-" + from + "-to-" + to + ".csv\"");
            // The BOM is what makes Excel open a UTF-8 CSV as UTF-8 rather than as the
            // system code page, which is how a name with an accent becomes mojibake in a
            // spreadsheet.
            response.getOutputStream().write(0xEF);
            response.getOutputStream().write(0xBB);
            response.getOutputStream().write(0xBF);
            response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        });
    }
}
