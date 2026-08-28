package com.sunrise.clinic.access.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registration form's validation contract, asserted against the files
 * themselves. Same style as {@link AbstractLoginServletTest}: no container, no
 * browser — the property that matters here is that the four required fields
 * carry the same browser-side checks the email field already had, so an
 * invalid submission is blocked on the form (field bubble, no server round
 * trip) and, when something still reaches the server, the form re-renders
 * with the reason and the typed values rather than dumping to the error page.
 *
 * <p>The gap this guards: only the email field showed browser validation,
 * because it was the only field with an HTML5 constraint. Leave a name out and
 * the request went to the server, which answered with the generic error page —
 * so the person typed everything again.</p>
 */
class RegisterPageTest {

    private static final String JSP = "src/main/webapp/WEB-INF/jsp/access/register.jsp";
    private static final String SERVLET =
            "src/main/java/com/sunrise/clinic/access/web/RegisterServlet.java";

    @Test
    void theFourRequiredFieldsAllCarryRequired() {
        String form = read(JSP);
        for (String name : new String[]{"name", "email", "password", "confirmPassword"}) {
            assertTrue(hasAttribute(form, "id=\"" + name + "\"", "required"),
                    name + " must be required, like the email field already was");
        }
    }

    @Test
    void thePasswordCarriesItsLengthRule() {
        String form = read(JSP);
        assertTrue(hasAttribute(form, "id=\"password\"", "minlength=\"8\""),
                "the minimum length belongs in the browser check, not just the server");
    }

    @Test
    void theEmailFieldKeepsItsFormatType() {
        String form = read(JSP);
        assertTrue(hasAttribute(form, "id=\"email\"", "type=\"email\""),
                "the email field's native format bubble is the pattern every field mirrors");
    }

    @Test
    void theOptionalFieldsStayOptional() {
        String form = read(JSP);
        for (String name : new String[]{"contactNumber", "dob", "address"}) {
            assertFalse(hasAttribute(form, "id=\"" + name + "\"", "required"),
                    name + " is optional and must not get a required bubble");
        }
    }

    @Test
    void theTypedValuesComeBackIntoTheFields() {
        // The re-render on invalid input is only a mercy if what the person typed
        // survives it. Each re-populated field reads the submitted parameter.
        String form = read(JSP);
        for (String name : new String[]{"name", "email"}) {
            assertTrue(hasAttribute(form, "id=\"" + name + "\"", "value=\"<c:out value='${param." + name + "}' />\""),
                    name + " must re-populate from the request parameter after a failed submit");
        }
    }

    @Test
    void aRejectedSubmitReRendersTheFormInsteadOfTheErrorPage() {
        // The servlet catches IllegalArgumentException and forwards back to the
        // register view with an "error" attribute the JSP shows above the form —
        // the same re-render pattern the login portals use. Landing on the error
        // page would make a person type everything again.
        String servlet = read(SERVLET);
        assertTrue(servlet.contains("render(request, response, \"access/register\")"),
                "invalid input must re-render the form, not the error page");
        assertTrue(servlet.contains("request.setAttribute(\"error\", problem.getMessage())"),
                "the re-render must carry the reason to the form");
    }

    private static boolean hasAttribute(String haystack, String anchor, String attribute) {
        int at = haystack.indexOf(anchor);
        if (at < 0) {
            return false;
        }
        // Scan the whole line holding the anchor (some attributes precede id=) and
        // the few lines after it (the contact field spreads its attributes).
        int lineStart = haystack.lastIndexOf('\n', at) + 1;
        int lineEnd = haystack.indexOf('\n', at);
        for (int i = 0; i < 4 && lineEnd >= 0; i++) {
            String line = haystack.substring(lineStart, lineEnd);
            if (line.contains(attribute)) {
                return true;
            }
            lineStart = lineEnd + 1;
            lineEnd = haystack.indexOf('\n', lineEnd + 1);
        }
        return false;
    }

    private static String read(String relative) {
        try {
            return java.nio.file.Files.readString(Path.of(relative));
        } catch (java.io.IOException e) {
            throw new AssertionError("cannot read " + relative, e);
        }
    }
}