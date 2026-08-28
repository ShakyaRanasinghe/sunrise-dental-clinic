package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.service.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Administrator's reports: income, the three-way revenue split, and footfall.
 *
 * <p>These are the only screens where the internal split is shown. Everywhere else
 * a bill is presented as the patient sees it — a total — because how the clinic
 * divides that money between the dentist, the practice and the front desk is not
 * the patient's business.</p>
 *
 * <p>Adding {@code ?export=csv} downloads the same figures as a spreadsheet. CSV is
 * generated directly rather than through a library; the one thing that genuinely
 * needs care is quoting, since an unescaped name containing a comma would silently
 * shift every following column.</p>
 */
public class AdminReportsServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            AccessControl.require(currentUser(request), Role.ADMIN);

            LocalDate to = dateField(request, "to", LocalDate.now());
            LocalDate from = dateField(request, "from", to.minusDays(29));

            Map<String, String> dentistNames = app().dentists().findAll().stream()
                    .collect(Collectors.toMap(d -> d.getId(), d -> d.getName(), (a, b) -> a));
            Map<String, String> staffNames = app().users().findStaff().stream()
                    .collect(Collectors.toMap(
                            u -> u.getUid(),
                            u -> u.getDisplayName() == null ? u.getEmail() : u.getDisplayName(),
                            (a, b) -> a));

            ReportService.IncomeReport income =
                    app().reportService().income(from, to, dentistNames, staffNames);
            ReportService.FootfallReport footfall = app().reportService().footfall(from, to);

            if ("csv".equalsIgnoreCase(request.getParameter("export"))) {
                writeCsv(response, income);
                return;
            }

            request.setAttribute("from", from);
            request.setAttribute("to", to);
            request.setAttribute("income", income);
            request.setAttribute("footfall", footfall);
            request.setAttribute("patientCount", app().patients().count());
            request.setAttribute("appointmentCount", app().appointments().count());

            render(request, response, "admin/reports");
        });
    }

    private void writeCsv(HttpServletResponse response, ReportService.IncomeReport income)
            throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"income-" + income.from() + "-to-" + income.to() + ".csv\"");

        PrintWriter out = response.getWriter();
        out.println("Sunrise Dental Clinic — income report");
        out.printf("Period,%s,to,%s%n", income.from(), income.to());
        out.println();
        out.println("Summary");
        out.printf("Bills issued,%d%n", income.billCount());
        out.printf("Gross total,%.2f%n", income.grossTotal());
        out.printf("Dentist earnings,%.2f%n", income.dentistEarnings());
        out.printf("Clinic earnings,%.2f%n", income.clinicEarnings());
        out.printf("Receptionist earnings,%.2f%n", income.receptionistEarnings());
        out.println();

        out.println("Earnings by dentist");
        out.println("Name,Appointments billed,Amount");
        for (ReportService.EarningsRow row : income.byDentist()) {
            out.printf("%s,%d,%.2f%n", csv(row.name()), row.count(), row.amount());
        }
        out.println();

        out.println("Earnings by receptionist");
        out.println("Name,Bills handled,Amount");
        for (ReportService.EarningsRow row : income.byReceptionist()) {
            out.printf("%s,%d,%.2f%n", csv(row.name()), row.count(), row.amount());
        }
        out.println();

        out.println("Daily takings");
        out.println("Date,Bills,Amount");
        for (ReportService.DailyPoint point : income.daily()) {
            out.printf("%s,%d,%.2f%n", point.date(), point.count(), point.amount());
        }
    }

    /**
     * Quote a CSV field. A value containing a comma, quote or newline is wrapped in
     * quotes, with any embedded quote doubled — the escaping rule from RFC 4180.
     */
    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
