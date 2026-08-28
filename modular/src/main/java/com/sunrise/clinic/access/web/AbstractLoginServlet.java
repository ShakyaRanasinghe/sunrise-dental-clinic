package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.RolePolicy;
import com.sunrise.clinic.access.service.AuthService;
import com.sunrise.clinic.platform.web.PageServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The whole sign-in sequence, written once for four portals.
 *
 * <p>Template Method (FR-OOP-06, FR-OOP-11). {@link #doPost} is final: the
 * algorithm is fixed and a subclass supplies only which role it admits and which
 * view it renders. Four independent servlets would have repeated these twenty
 * lines four times, and <strong>the fourth copy is where the role check gets
 * forgotten</strong> — which is the entire argument for doing it this way rather
 * than for elegance.</p>
 *
 * <p>The sequence:</p>
 * <ol>
 *   <li>read the credentials</li>
 *   <li>{@link AuthService#login} — the existing service, unchanged</li>
 *   <li>failed, or the role does not match this portal → re-render with the
 *       <em>same</em> message either way</li>
 *   <li>establish the session</li>
 *   <li>redirect to the role's own home path</li>
 * </ol>
 *
 * <p>Step 3 is the one worth reading twice. A portal that said "wrong portal"
 * would be an oracle: type an address into {@code /login/admin} and the error
 * tells you whether it is an administrator. So a role mismatch is reported
 * exactly as a wrong password is, and the credentials are verified <em>before</em>
 * the role is checked so that the work done is the same on both paths
 * (FR-AUTH-03).</p>
 */
public abstract class AbstractLoginServlet extends PageServlet {

    private static final Logger log = Logger.getLogger(AbstractLoginServlet.class.getName());

    /** Shown for a wrong password and for a wrong portal alike. */
    private static final String REJECTED = "Incorrect email or password.";

    // ------------------------------------------------------------------
    //  What a subclass must supply
    // ------------------------------------------------------------------

    /** The one role this portal admits. */
    protected abstract Role acceptedRole();

    /** The view to render, under {@code /WEB-INF/jsp/}. */
    protected abstract String viewName();

    // ------------------------------------------------------------------
    //  What a subclass may override
    // ------------------------------------------------------------------

    /** Whether this portal offers a route to create an account. Patients only. */
    protected boolean allowsSelfRegistration() {
        return false;
    }

    /** Whether a failed attempt here is worth an audit line. Administrators. */
    protected boolean auditFailedAttempts() {
        return false;
    }

    // ------------------------------------------------------------------
    //  The algorithm, fixed
    // ------------------------------------------------------------------

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        if (currentUser(request) != null) {
            // Already signed in: send them where they belong rather than
            // offering a form that would replace a working session.
            redirect(request, response, currentUser(request).policy().homePath());
            return;
        }
        renderForm(request, response, null, null);
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");

        if (email.isEmpty() || password == null || password.isEmpty()) {
            renderForm(request, response, "Enter your email address and password.", email);
            return;
        }

        AuthService.LoginResult result = app().authService().login(email, password);

        if (!result.success()) {
            if (auditFailedAttempts()) {
                log.log(Level.WARNING, "admin_portal_login_failed email={0}", email);
            }
            // The service's own message is used here because it distinguishes a
            // locked account, which the holder needs to be told about.
            renderForm(request, response, result.message(), email);
            return;
        }

        if (result.user().getRole() != acceptedRole()) {
            // Correct password, wrong portal. Same message, same work done.
            log.log(Level.INFO, "wrong_portal email={0} role={1} portal={2}",
                    new Object[]{email, result.user().getRole(), acceptedRole()});
            renderForm(request, response, REJECTED, email);
            return;
        }

        ClinicPrincipal principal = new ClinicPrincipal(
                result.user().getUid(), result.user().getDisplayName(), result.user().getRole());
        AuthenticationFilter.establishSession(request, principal);

        log.log(Level.INFO, "login_success email={0} role={1}",
                new Object[]{email, acceptedRole()});
        redirect(request, response, RolePolicy.of(acceptedRole()).homePath());
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            String error, String email) throws IOException, jakarta.servlet.ServletException {
        request.setAttribute("error", error);
        request.setAttribute("email", email == null ? "" : email);
        // Where the form posts back to. It cannot be read from the request: this
        // JSP is reached by a forward, so getServletPath() returns the view's own
        // path, and the form posted to /WEB-INF/jsp/access/login-dentist.jsp -
        // which Tomcat refuses, so sign-in from a browser answered 404 while the
        // same credentials over curl worked, because curl posts to the URL and
        // never reads the form's action.
        request.setAttribute("loginPath", RolePolicy.of(acceptedRole()).loginPath());
        request.setAttribute("allowsSelfRegistration", allowsSelfRegistration());
        request.setAttribute("portalLabel", portalLabel());
        render(request, response, viewName());
    }

    /** The label shown above the form, derived from the role. */
    private String portalLabel() {
        return switch (acceptedRole()) {
            case PATIENT -> "Patient";
            case RECEPTIONIST -> "Reception";
            case DENTIST -> "Dentist";
            case ADMIN -> "Administrator";
        };
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
