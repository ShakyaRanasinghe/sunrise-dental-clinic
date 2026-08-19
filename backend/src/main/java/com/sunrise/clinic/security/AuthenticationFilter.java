package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.json.Json;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Establishes who is making the request, and turns anonymous users away from
 * protected paths.
 *
 * <p>This replaces the security framework's filter chain. It runs before every
 * request and does three things:</p>
 *
 * <ol>
 *   <li>reads the {@link ClinicPrincipal} out of the HTTP session, if the user has
 *       signed in, and republishes it as a request attribute so servlets and JSP
 *       pages can reach it without touching the session directly;</li>
 *   <li>lets public paths (login, registration, static assets) through
 *       unconditionally;</li>
 *   <li>rejects everything else when there is no principal — redirecting a browser
 *       to the login page, but answering an API call with a 401 JSON body, since a
 *       redirect to HTML is useless to a caller expecting JSON.</li>
 * </ol>
 *
 * <p>Role checks are intentionally <em>not</em> done here. Which roles may perform
 * which action is decided in each servlet through {@link AccessControl}, keeping
 * the rule next to the operation it protects instead of in a list of URL patterns
 * that quietly drifts out of step with the code.</p>
 */
public class AuthenticationFilter implements Filter {

    /** Paths reachable without signing in. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/login", "/logout", "/register", "/help",
            "/css/", "/js/", "/images/", "/favicon.ico",
            "/api/auth/");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        ClinicPrincipal principal = currentPrincipal(request);
        if (principal != null) {
            request.setAttribute(ClinicPrincipal.REQUEST_KEY, principal);
        }

        String path = pathWithinApplication(request);
        if (principal != null || isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(Json.write(Map.of(
                    "errorCode", "unauthenticated",
                    "message", "Authentication is required.")));
            return;
        }

        // Remember where they were heading so login can send them back.
        response.sendRedirect(request.getContextPath() + "/login?next="
                + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8));
    }

    /** @return the signed-in user, or null. */
    public static ClinicPrincipal currentPrincipal(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(ClinicPrincipal.REQUEST_KEY);
        if (fromRequest instanceof ClinicPrincipal principal) {
            return principal;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object stored = session.getAttribute(ClinicPrincipal.SESSION_KEY);
        return stored instanceof ClinicPrincipal principal ? principal : null;
    }

    /** Sign a user in: start a fresh session and remember them in it. */
    public static void establishSession(HttpServletRequest request, ClinicPrincipal principal) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            // A new session id on login prevents session-fixation attacks.
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(ClinicPrincipal.SESSION_KEY, principal);
        session.setMaxInactiveInterval((int) java.time.Duration.ofMinutes(30).toSeconds());
    }

    /** Sign a user out. */
    public static void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** @return the landing page for a role, used after a successful login. */
    public static String homeFor(Role role) {
        return switch (role) {
            case PATIENT -> "/patient/home";
            case RECEPTIONIST -> "/reception/home";
            case DENTIST -> "/dentist/schedule";
            case ADMIN -> "/admin/reports";
        };
    }

    private static boolean isPublic(String path) {
        if (path.isEmpty() || "/".equals(path)) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }
}
