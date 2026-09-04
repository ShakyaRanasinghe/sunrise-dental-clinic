package com.sunrise.clinic.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The v1.0.2 UX polish batch (GAP-PAT-22..29, GAP-FTB-11..14), asserted against
 * the files themselves — the same style as {@code RegisterPageTest} and
 * {@code PageServletViewsTest}. No container, no browser: each property that
 * matters is a string a future edit could silently drop, so each gets a test.
 */
class V102UxPolishTest {

    private static final Path JSP = Path.of("src/main/webapp/WEB-INF/jsp");
    private static final Path JAVA = Path.of("src/main/java");
    private static final Path WEB_XML = Path.of("src/main/webapp/WEB-INF/web.xml");
    private static final Path CSS = Path.of("src/main/webapp/css/app.css");

    // GAP-PAT-22: no booking call-to-action under a public service.
    @Test
    void publicServicesCarryNoBookLink() {
        String home = read(JSP.resolve("access/home.jsp"));
        assertFalse(home.contains("Book this service"),
                "a service description must not invite a sign-up from the middle of the page");
        assertFalse(home.contains("service-item__cta"),
                "the removed call-to-action must leave no class behind");
    }

    // Dentist cards fill evenly: the grey body stretches so a dentist without a
    // phone number never ends with a white stump.
    @Test
    void dentistCardsFillEvenly() {
        String css = read(CSS);
        assertTrue(css.contains(".dentist-card__body"),
                "the card body style must exist");
        int card = css.indexOf(".dentist-card {");
        assertTrue(card >= 0 && css.indexOf("flex-direction: column", card) > card,
                "the card must stack as a flex column");
        int body = css.indexOf(".dentist-card__body {");
        assertTrue(body >= 0 && css.indexOf("flex: 1", body) > body,
                "the grey body must stretch to fill short cards");
    }

    // Public dentist cards: below the publishable threshold no rating metric
    // shows at all — neither stars nor a "more reviews needed" hint.
    @Test
    void unratedDentistsShowNoRatingHint() {
        String home = read(JSP.resolve("access/home.jsp"));
        assertFalse(home.contains("needed to show rating"),
                "a dentist without a publishable rating must show no rating hint");
        assertTrue(home.contains("rating.isPublishable()"),
                "dentists with a publishable rating must still show it");
    }

    // Compact register search: no heading, no guide texts, tight card.
    @Test
    void registerSearchIsCompact() {
        String page = read(JSP.resolve("patients/register.jsp"));
        assertFalse(page.contains("One field covers all three"),
                "the search guide text must be gone");
        assertFalse(page.contains("Name, contact number or email"),
                "the long field label must be gone");
        assertTrue(page.contains("search-card"),
                "the search card must use the compact style");
    }

    // GAP-PAT-31: booking with nothing to offer says so instead of blank.
    @Test
    void bookingNamesNoDoctors() {
        String book = read(JSP.resolve("appointments/book.jsp"));
        assertTrue(book.contains("No doctors available"),
                "an empty overview must render a no-doctors card");
        String walkin = read(JSP.resolve("scheduling/walkin.jsp"));
        assertTrue(walkin.contains("There are no available doctors today"),
                "walk-in with no active doctors must say so");
    }

    // GAP-PAT-23: dentist cards render whole, with no expand toggle.
    @Test
    void dentistCardsAreFullyDisplayed() {
        String home = read(JSP.resolve("access/home.jsp"));
        assertFalse(home.contains("<details class=\"dentist-card\""),
                "dentist cards must not hide behind a details toggle");
        assertTrue(home.contains("<div class=\"dentist-card\">"),
                "each dentist renders as a full card");
    }

    // GAP-FTB-11: the footer names the build a Version, not a Release.
    @Test
    void footerSaysVersion() {
        String footer = read(JSP.resolve("shared/footer.jspf"));
        assertTrue(footer.contains("Version v"),
                "the footer must read 'Version v…'");
        assertFalse(footer.contains("Release v"),
                "the old 'Release v…' label must be gone");
    }

    // GAP-FTB-13: the public help contents card reads plainly.
    @Test
    void publicHelpTopicsLabel() {
        String help = read(JSP.resolve("shared/help.jsp"));
        assertTrue(help.contains("Help topics"), "the contents card must be headed plainly");
        assertFalse(help.contains("Jump to a topic"), "the old label must be gone");
    }

    // GAP-FTB-12: one help page per role, served from the public /help prefix.
    @Test
    void everyRoleHasItsOwnHelpPage() {
        for (String role : new String[]{"patient", "reception", "dentist", "admin"}) {
            Path view = JSP.resolve("shared/help-" + role + ".jsp");
            assertTrue(Files.exists(view), "missing role help page: " + view);
            String body = read(view);
            assertTrue(body.contains("Help topics"),
                    view + " must carry the same contents-card pattern as the shared page");
        }
    }

    @Test
    void helpServletServesTheRolePages() throws IOException {
        String servlet = read(JAVA.resolve(
                "com/sunrise/clinic/platform/web/HelpServlet.java"));
        for (String role : new String[]{"patient", "reception", "dentist", "admin"}) {
            assertTrue(servlet.contains("\"shared/help-" + role + "\""),
                    "HelpServlet must dispatch /help/" + role + " to its own view");
        }
        String webXml = read(WEB_XML);
        assertTrue(webXml.contains("<url-pattern>/help/*</url-pattern>"),
                "web.xml must map /help/* so the role pages resolve");
    }

    // GAP-PAT-24 + GAP-FTB-12: every signed-in nav ends in its own Help.
    @Test
    void everyRoleNavEndsInHelp() {
        assertNavHelp("PatientPolicy.java", "/help/patient");
        assertNavHelp("ReceptionPolicy.java", "/help/reception");
        assertNavHelp("DentistPolicy.java", "/help/dentist");
        assertNavHelp("AdminPolicy.java", "/help/admin");
    }

    private static void assertNavHelp(String policy, String path) {
        String source = read(JAVA.resolve("com/sunrise/clinic/access/domain/" + policy));
        assertTrue(source.contains("new NavItem(\"Help\", \"" + path + "\")"),
                policy + " must link Help to " + path);
    }

    // GAP-FTB-12: sign-in screens link to their own help; the staff portal
    // offers none (each role's help differs, so a shared one misleads).
    @Test
    void signInScreensLinkToTheirOwnHelp() {
        assertTrue(read(JSP.resolve("access/login-patient.jsp")).contains("/help\""),
                "patient sign-in keeps the shared help link");
        assertTrue(read(JSP.resolve("access/login-reception.jsp")).contains("/help/reception"),
                "reception sign-in must link its own help");
        assertTrue(read(JSP.resolve("access/login-dentist.jsp")).contains("/help/dentist"),
                "dentist sign-in must link its own help");
        assertTrue(read(JSP.resolve("access/login-admin.jsp")).contains("/help/admin"),
                "admin sign-in must link its own help");
        assertFalse(read(JSP.resolve("access/staff-portal.jsp")).contains("/help"),
                "the staff portal must offer no help link");
    }

    // GAP-PAT-25: the concern page leads with the visits, not an explainer.
    @Test
    void concernPageHasNoIntroBlock() {
        String page = read(JSP.resolve("feedback/patient-complaints.jsp"));
        assertFalse(page.contains("Who reads this:"),
                "the 'Who reads this' notice block must be gone");
        assertFalse(page.contains("Choose the dentist your concern is about"),
                "the chooser preamble must be gone");
    }

    // GAP-PAT-26: a wider card, two fields per row, no surplus sentences.
    @Test
    void registerFormIsWideAndShort() {
        String form = read(JSP.resolve("access/register.jsp"));
        assertTrue(form.contains("register-wide"), "the card must use the wider layout");
        assertFalse(form.contains("Nothing medical is asked here"),
                "the 'Nothing medical' notice must be gone");
        assertFalse(form.contains("So the clinic can reach you about an appointment"),
                "the phone helper sentence must be gone");
    }

    // GAP-FTB-14: `editing` is a Boolean set by the servlet, so the view must
    // test its value — `not empty` on a Boolean is always true, which once
    // rendered the edit form on every visit and left the read-only view dead.
    @Test
    void profileEditGateTestsTheBoolean() {
        String profile = read(JSP.resolve("patients/profile.jsp"));
        assertTrue(profile.contains("<c:when test=\"${editing}\">"),
                "the edit form must render only when editing is true");
        assertFalse(profile.contains("not empty editing"),
                "`not empty` on a Boolean never gates anything");
    }

    // GAP-PAT-27 + GAP-FTB-14: diagnosis details live on the profile, and a
    // saved change confirms in a sub-window.
    @Test
    void profileCarriesDiagnosisDetailsAndConfirms() {
        String profile = read(JSP.resolve("patients/profile.jsp"));
        assertTrue(profile.contains("name=\"diagnosisDetails\""),
                "the profile edit form must accept diagnosis details");
        assertTrue(profile.contains("profile.diagnosisDetails()"),
                "the profile must show stored diagnosis details");
        assertTrue(profile.contains("Successfully updated"),
                "a saved profile must confirm in a sub-window");
    }

    // GAP-PAT-28: the button names the dentist; no re-picker card; slots first.
    @Test
    void bookingNamesTheDentistAndLeadsWithSlots() {
        String book = read(JSP.resolve("appointments/book.jsp"));
        assertTrue(book.contains("Proceed appointment with"),
                "the submit must name the chosen dentist");
        assertFalse(book.contains("Confirm booking"), "the old submit label must be gone");
        assertFalse(book.contains("<h2>2. Dentist and date</h2>"),
                "the redundant picker card must be gone");
        int slots = book.indexOf("Available times with");
        int treatments = book.indexOf("Treatment for this visit");
        assertTrue(slots >= 0 && treatments > slots,
                "the times must head the card with the treatment picker below them");
    }

    // GAP-PAT-29: every offered treatment opens its description.
    @Test
    void treatmentsCarryInfoMarks() {
        String book = read(JSP.resolve("appointments/book.jsp"));
        assertTrue(book.contains("class=\"treatment-info\""),
                "each treatment needs an info mark");
        assertTrue(book.contains("id=\"treatment-${t.id()}\""),
                "each treatment needs its description sub-window");
        assertTrue(read(CSS).contains(".treatment-info"),
                "the info mark needs its style");
    }

    // GAP-FTB-14: every update point confirms in a sub-window.
    @Test
    void updatePointsConfirmInASubWindow() {
        assertTrue(read(CSS).contains(".sub-window.open"),
                "post-redirect confirmations need the always-visible sub-window style");
        for (String page : new String[]{
                "patients/profile.jsp",
                "appointments/dentist-availability.jsp",
                "reporting/clinic-identity.jsp",
                "reporting/treatments.jsp",
                "appointments/patient-home.jsp",
                "appointments/reception-day.jsp",
                "appointments/dentist-schedule.jsp",
                "patients/register.jsp",
                "feedback/patient-complaints.jsp",
                "feedback/admin-complaints.jsp",
                "scheduling/availability.jsp",
                "scheduling/walkin.jsp",
                "reporting/accounts.jsp"}) {
            assertTrue(read(JSP.resolve(page)).contains("sub-window open"),
                    page + " must confirm a save in a sub-window");
        }
    }

    // The schedule's coming-week section reads "Upcoming appointments".
    @Test
    void scheduleWeekSectionIsPlainlyLabelled() {
        String schedule = read(JSP.resolve("appointments/dentist-schedule.jsp"));
        assertTrue(schedule.contains("Upcoming appointments"),
                "the coming-week section must say what it is");
        assertFalse(schedule.contains("<h2 class=\"page-title\">The week ahead</h2>"),
                "the vague heading must be gone");
    }

    // GAP-DEN-13: completing a treatment-less visit asks the dentist for the price.
    @Test
    void scheduleAsksPriceForTreatmentLessVisits() {
        String schedule = read(JSP.resolve("appointments/dentist-schedule.jsp"));
        assertTrue(schedule.contains("name=\"customPrice\""),
                "the record form must accept the dentist-entered price");
        assertTrue(schedule.contains("empty a.treatmentId"),
                "the price field must show only where no catalog treatment exists");
    }

    // GAP-REC-13: desk screens fall back to the patient's stated reason.
    @Test
    void deskScreensFallBackToPatientReason() {
        assertTrue(read(JSP.resolve("billing/billing.jsp")).contains("patientReason()"),
                "the billing list must name the reason on treatment-less rows");
    }

    // GAP-PAT-30: the patient's own receipt views show the dentist's description,
    // gated so reception and admin never see it on the shared receipt page.
    @Test
    void patientReceiptViewsShowDentistNote() {
        String receipt = read(JSP.resolve("billing/receipt.jsp"));
        assertTrue(receipt.contains("showClinical"),
                "the shared receipt must gate the clinical row on the viewer");
        assertTrue(receipt.contains("bill.diagnosis()"),
                "the patient's copy must print the dentist's note");
        assertTrue(read(JSP.resolve("appointments/patient-home.jsp")).contains("a.diagnosis()"),
                "the dashboard receipt view must print the dentist's note");
    }

    // The phone editor lives on the dentist profile now, not on availability.
    @Test
    void availabilityCarriesNoPhoneCard() {
        String page = read(JSP.resolve("appointments/dentist-availability.jsp"));
        assertFalse(page.contains("Contact details"),
                "the phone card moved to My details");
        assertFalse(page.contains("name=\"phone\""),
                "no phone field may remain on the availability screen");
    }

    // GAP-DEN-14: the dentist's own profile — read-only with an Edit gate that
    // tests the Boolean (cf. the old profile bug), fee view-only, saves confirm.
    @Test
    void dentistProfileIsReadOnlyWithAnEditGate() {
        String profile = read(JSP.resolve("scheduling/dentist-profile.jsp"));
        assertTrue(profile.contains("<c:when test=\"${editing}\">"),
                "the edit form must render only when editing is true");
        assertFalse(profile.contains("not empty editing"),
                "`not empty` on a Boolean never gates anything");
        assertTrue(profile.contains("Successfully updated"),
                "a saved profile must confirm in a sub-window");
        assertTrue(read(JSP.resolve("shared/header.jspf")).contains("/dentist/profile"),
                "the account menu must offer the dentist My details");
        assertTrue(read(WEB_XML).contains("<url-pattern>/dentist/profile</url-pattern>"),
                "web.xml must map the dentist profile");
    }

    // GAP-ADM-10: the Pricing tab exists, is wired, and carries no reception dial.
    @Test
    void pricingTabExists() {
        assertTrue(read(JSP.resolve("reporting/pricing.jsp")).contains("dentistSharePercent"),
                "the Pricing tab must edit the dentist share");
        assertTrue(read(JSP.resolve("reporting/pricing.jsp")).contains("Successfully updated"),
                "a saved pricing must confirm in a sub-window");
        assertTrue(read(WEB_XML).contains("<url-pattern>/admin/pricing</url-pattern>"),
                "web.xml must map the Pricing tab");
    }

    // GAP-ADM-10: reception earns no commission, so Reports shows no trace.
    @Test
    void reportsCarryNoReceptionMetric() {
        String reports = read(JSP.resolve("reporting/reports.jsp"));
        assertFalse(reports.contains("Reception handling"),
                "the handling stat must be gone");
        assertFalse(reports.contains("Per receptionist"),
                "the per-receptionist table must be gone");
    }

    // GAP-DEN-15: the dentist's profile states the configured share.
    @Test
    void dentistProfileStatesShare() {
        assertTrue(read(JSP.resolve("scheduling/dentist-profile.jsp"))
                        .contains("dentistSharePercent"),
                "the dentist profile must state the configured share");
    }

    // GAP-DEN-16: windows first with day totals and Full pills, treatments below.
    @Test
    void availabilityLeadsWithFriendlyWindows() {
        String page = read(JSP.resolve("appointments/dentist-availability.jsp"));
        int windows = page.indexOf("still open");
        int treatments = page.indexOf("Treatments I offer");
        assertTrue(windows >= 0 && windows < treatments,
                "the time windows must come before the treatments list");
        assertTrue(page.contains("pill open") && page.contains(">Full<"),
                "open windows read as pills and exhausted ones as Full");
    }

    // Records expose components as methods, not bean properties: ${empty a.x}
    // 500s at render while ${empty a.x()} tests the value. Scan every view.
    @Test
    void noRecordPropertyAccessInViews() throws IOException {
        java.util.regex.Pattern bad = java.util.regex.Pattern.compile(
                "\\$\\{(not )?empty [a-zA-Z_$][\\w$]*\\.[a-zA-Z_$][\\w$]*\\s*\\}");
        List<String> offenders = new ArrayList<>();
        try (var paths = Files.walk(JSP)) {
            for (Path file : paths.filter(f -> f.toString().endsWith(".jsp")).toList()) {
                String body = Files.readString(file);
                // JSP comments never evaluate — strip them before matching.
                body = body.replaceAll("(?s)<%--.*?--%>", "");
                if (bad.matcher(body).find()) {
                    offenders.add(JSP.relativize(file).toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "record property access (use method calls): " + offenders);
    }

    // An "Other" visit names its type on the patient dashboard, with the reason
    // beneath it — never a blank cell.
    @Test
    void dashboardNamesOtherVisits() {
        String home = read(JSP.resolve("appointments/patient-home.jsp"));
        assertTrue(home.contains("a.patientReason()"),
                "an Other row must show the stated reason under its type");
    }

    // GAP-PAT-32: a general concern needs no dentist and no history, and the
    // admin queue names neither identity.
    @Test
    void generalConcernsAreAnonymous() {
        String page = read(JSP.resolve("feedback/patient-complaints.jsp"));
        assertTrue(page.contains("name=\"action\" value=\"general\""),
                "the general form must post the general action");
        assertTrue(page.contains("Anonymous"),
                "the general form must say it is anonymous");
        String admin = read(JSP.resolve("feedback/admin-complaints.jsp"));
        assertTrue(admin.contains("Anonymous"),
                "the admin queue must label identity-less concerns");
    }

    // GAP-PAT-33/REC-14/ADM-11: quotable numbers where people are listed.
    @Test
    void peopleScreensListQuotableNumbers() {
        assertTrue(read(JSP.resolve("reporting/accounts.jsp")).contains("Account ID"),
                "the Accounts table must list the number leftmost");
        assertTrue(read(JSP.resolve("patients/register.jsp")).contains("patientNumber()"),
                "the register must show the desk number");
    }

    // GAP-ADM-12: staff portals ask for a username (patient keeps email), and the
    // administrator provisions and lists usernames.
    @Test
    void staffPortalsAskForUsernames() {
        String form = read(JSP.resolve("access/login-form.jspf"));
        assertTrue(form.contains("name=\"identity\""),
                "the shared form must post the identity field");
        assertTrue(form.contains("identityLabel"),
                "the label must come from the portal (Username vs Email address)");
        String base = read(JAVA.resolve("com/sunrise/clinic/access/web/AbstractLoginServlet.java"));
        assertTrue(base.contains("\"Username\"") && base.contains("\"Email address\""),
                "the base servlet must derive the label from the role");
        String accounts = read(JSP.resolve("reporting/accounts.jsp"));
        assertTrue(accounts.contains("name=\"username\""),
                "staff creation must ask for a username");
        assertTrue(accounts.contains("${a.username()}"),
                "the Accounts table must show usernames");
    }

    // GAP-DEN-12: pending patients are expandable cards; the record form lives
    // only inside the opened card, with the first one open.
    @Test
    void schedulePendingPatientsAreExpandable() {
        String schedule = read(JSP.resolve("appointments/dentist-schedule.jsp"));
        assertTrue(schedule.contains("<details class=\"card pending-list\""),
                "each pending patient must be an expandable card");
        assertTrue(schedule.contains("pending-list__body"),
                "details and form must render inside the opened card only");
        assertTrue(read(CSS).contains(".pending-list"),
                "the expandable cards need their style");
    }

    // GAP-FTB-14: the two silent updates now carry a confirmation flag.
    @Test
    void silentUpdatesCarryConfirmationFlags() {
        String availability = read(JAVA.resolve(
                "com/sunrise/clinic/appointments/web/DentistAvailabilityServlet.java"));
        assertTrue(availability.contains("toggled=1"),
                "a flipped treatment toggle must redirect with a confirmation flag");
        String walkin = read(JAVA.resolve(
                "com/sunrise/clinic/scheduling/web/WalkInServlet.java"));
        assertTrue(walkin.contains("registered=1"),
                "a walk-in registration must redirect with a confirmation flag");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + file, e);
        }
    }
}
