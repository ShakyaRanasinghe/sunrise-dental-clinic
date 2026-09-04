package com.sunrise.clinic.reporting.web;

import com.sunrise.clinic.platform.service.ClinicIdentityService;
import com.sunrise.clinic.platform.web.PageServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Administrator screen for pricing (GAP-ADM-10) — the dentist's share of each
 * treatment price, and the flat service charge per bill.
 *
 * <p>These dials were previously locked in {@code clinic.properties} and only
 * changeable by editing the file and redeploying. They are now stored in the
 * {@code clinic_setting} table and read live on every bill, so a save here
 * applies to the next bill with no restart. Bills already issued keep the split
 * they were issued with. Both fields require
 * {@link com.sunrise.clinic.access.domain.Action#MANAGE_CLINIC_SETTINGS},
 * which only the administrator holds.</p>
 */
public class PricingServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> show(request, response));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            ClinicIdentityService svc = app().clinicIdentity();
            // The tab posts percent ("60"); the service stores the fraction ("0.60").
            String percent = request.getParameter("dentistSharePercent");
            if (percent != null) {
                svc.update(currentUser(request),
                        ClinicIdentityService.DENTIST_SHARE_KEY, percent);
            }
            String charge = request.getParameter("serviceCharge");
            if (charge != null) {
                svc.update(currentUser(request),
                        ClinicIdentityService.SERVICE_CHARGE_KEY, charge);
            }
            redirect(request, response, "/admin/pricing?saved=1");
        });
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ClinicIdentityService svc = app().clinicIdentity();
        request.setAttribute("dentistSharePercent", percent(svc.dentistShare()));
        request.setAttribute("serviceCharge", svc.serviceCharge().toPlainString());
        request.setAttribute("saved", field(request, "saved"));
        render(request, response, "reporting/pricing");
    }

    /** A 0..1 fraction as whole percent for the tab ("0.60" → "60"). */
    static String percent(BigDecimal fraction) {
        return fraction.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
