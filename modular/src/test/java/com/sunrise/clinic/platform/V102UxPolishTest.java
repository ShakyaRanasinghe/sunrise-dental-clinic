package com.sunrise.clinic.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                "reporting/treatments.jsp"}) {
            assertTrue(read(JSP.resolve(page)).contains("Successfully updated"),
                    page + " must confirm a save in a sub-window");
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + file, e);
        }
    }
}
