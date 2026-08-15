package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.UserAccount;
import com.sunrise.clinic.security.AuthService;
import com.sunrise.clinic.security.AuthenticationFilter;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The login page.
 *
 * <p>{@code GET} shows the form; {@code POST} verifies the credentials and, on
 * success, starts a session and sends the user to the landing page for their role.
 * A failed attempt re-renders the form with a message and the number of tries
 * remaining, so someone who has mistyped their password knows how close they are to
 * being locked out.</p>
 *
 * <p>On success the visitor is returned to whatever they were originally trying to
 * reach, which {@code AuthenticationFilter} passed along as {@code next}. The value
 * is only honoured if it is a path within this application — accepting an arbitrary
 * URL here would turn the login page into an open redirect that a phishing link
 * could bounce through.</p>
 */
public class LoginServlet extends PageServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ClinicPrincipal existing = currentUser(request);
        if (existing != null) {
            redirect(request, response, AuthenticationFilter.homeFor(existing.role()));
            return;
        }
        request.setAttribute("next", safeNext(request.getParameter("next")));
        render(request, response, "login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        page(request, response, () -> {
            String email = requiredField(request, "email", "Email");
            String password = requiredField(request, "password", "Password");
            String next = safeNext(request.getParameter("next"));

            AuthService.LoginResult result = app().authService().login(email, password);

            if (!result.success()) {
                request.setAttribute("error", result.message());
                request.setAttribute("email", email);
                request.setAttribute("next", next);
                if (!result.lockStatus().locked() && result.lockStatus().attemptsRemaining() <= 2) {
                    request.setAttribute("warning",
                            result.lockStatus().attemptsRemaining()
                                    + " attempt(s) remaining before this account is locked.");
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                render(request, response, "login");
                return;
            }

            UserAccount user = result.user();
            AuthenticationFilter.establishSession(request,
                    new ClinicPrincipal(user.getUid(), user.getDisplayName(), user.getRole()));

            redirect(request, response,
                    next != null ? next : AuthenticationFilter.homeFor(user.getRole()));
        });
    }

    /**
     * @return {@code next} if it is a safe in-application path, otherwise null.
     * A value must start with a single '/' — {@code //host} and {@code http://host}
     * are both absolute URLs to somewhere else and are rejected.
     */
    private static String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        String trimmed = next.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return null;
        }
        return trimmed;
    }
}
