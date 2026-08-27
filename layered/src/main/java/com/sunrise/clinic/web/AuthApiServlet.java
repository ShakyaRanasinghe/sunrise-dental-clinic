package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.UserAccount;
import com.sunrise.clinic.json.Json;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.AuthService;
import com.sunrise.clinic.security.AuthenticationFilter;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Authentication for API clients.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code POST /api/auth/login}</td><td>sign in, creating a session</td></tr>
 *   <tr><td>{@code POST /api/auth/logout}</td><td>sign out</td></tr>
 *   <tr><td>{@code POST /api/auth/register}</td><td>patient self-registration</td></tr>
 *   <tr><td>{@code GET  /api/auth/me}</td><td>who am I</td></tr>
 *   <tr><td>{@code GET  /api/auth/lock-status?email=}</td><td>lock-out state</td></tr>
 *   <tr><td>{@code POST /api/auth/unlock}</td><td>admin unlock</td></tr>
 * </table>
 *
 * <p>The previous version of this endpoint took the front-end's word for whether a
 * login had succeeded, because the actual password check happened in an external
 * service. Now the password is verified here, so {@code /login} both authenticates
 * and establishes the session, and the client can no longer report an outcome that
 * did not happen.</p>
 */
public class AuthApiServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            String action = path.isEmpty() ? "" : path.get(0);
            switch (action) {
                case "login" -> login(request, response);
                case "logout" -> logout(request, response);
                case "register" -> register(request, response);
                case "unlock" -> unlock(request, response);
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            String action = path.isEmpty() ? "" : path.get(0);
            switch (action) {
                case "me" -> me(request, response);
                case "lock-status" -> lockStatus(request, response);
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = readBody(request);
        String email = required(body, "email");
        String password = required(body, "password");

        AuthService.LoginResult result = app().authService().login(email, password);
        if (!result.success()) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of(
                    "errorCode", result.lockStatus().locked() ? "account_locked" : "invalid_credentials",
                    "message", result.message(),
                    "lockStatus", result.lockStatus()));
            return;
        }

        UserAccount user = result.user();
        ClinicPrincipal principal =
                new ClinicPrincipal(user.getUid(), user.getDisplayName(), user.getRole());
        AuthenticationFilter.establishSession(request, principal);

        writeJson(response, Map.of(
                "uid", user.getUid(),
                "displayName", user.getDisplayName() == null ? "" : user.getDisplayName(),
                "role", user.getRole(),
                "home", AuthenticationFilter.homeFor(user.getRole())));
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthenticationFilter.clearSession(request);
        writeJson(response, Map.of("message", "Signed out"));
    }

    /**
     * Self-registration always creates a PATIENT. Staff accounts are created by an
     * administrator, so a role supplied in the request body is deliberately ignored —
     * otherwise anyone could register themselves as an administrator.
     */
    private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = readBody(request);
        UserAccount created = app().authService().register(
                required(body, "email"),
                required(body, "password"),
                required(body, "displayName"),
                Role.PATIENT);
        writeJson(response, HttpServletResponse.SC_CREATED, Map.of(
                "uid", created.getUid(),
                "email", created.getEmail(),
                "role", created.getRole()));
    }

    private void me(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClinicPrincipal user = currentUser(request);
        if (user == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    Map.of("errorCode", "unauthenticated", "message", "Not signed in."));
            return;
        }
        writeJson(response, user);
    }

    private void lockStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = requiredParam(request, "email").toLowerCase();
        writeJson(response, app().loginAttemptService().status(email));
    }

    private void unlock(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccessControl.require(currentUser(request), Role.ADMIN);
        Map<String, Object> body = readBody(request);
        String email = required(body, "email").toLowerCase();
        app().authService().unlock(email);
        writeJson(response, app().loginAttemptService().status(email));
    }

    private static String required(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
