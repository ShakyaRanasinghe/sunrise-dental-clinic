package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.RolePolicy;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.json.Json;

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
 * <p>It also enforces one <em>coarse</em> role rule: a path a role owns may only be
 * entered by that role, so a signed-in patient cannot open {@code /admin/reports}.
 * The prefixes come from {@link RolePolicy#ownedPrefix()}, derived from each role's
 * own home path, so this is not a second list of URL patterns to drift out of step
 * with the code.</p>
 *
 * <p><b>Fine-grained</b> checks stay in the servlets, through {@link AccessControl},
 * next to the operation they protect - whether this receptionist may cancel <em>this</em>
 * appointment is not a question about a URL. The two are deliberately layered: the
 * filter is a structural guarantee that holds for a servlet whose author forgot to
 * call {@code require}, and the servlet check is the one that knows the specifics.</p>
 *
 * <p>This was found by testing rather than reading: at step 2 a signed-in patient
 * could load all four role landing pages with a 200, because the temporary
 * {@code StubHomeServlet} checked nothing. Relying on every one of thirty-two
 * servlets to remember is the discipline-not-construction problem this codebase
 * avoids elsewhere.</p>
 */
public class AuthenticationFilter implements Filter {

    /** Paths reachable without signing in. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/login",          // the chooser and all four portals beneath it
            "/logout",
            "/register",
            "/help",
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

        if (principal != null) {
            String owner = roleOwning(path);
            if (owner != null && !owner.equals(principal.role().name())) {
                refuse(request, response, path, principal);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (isPublic(path)) {
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
    /**
     * Where a role lands after signing in.
     *
     * <p>Delegates to the role's own {@link com.sunrise.clinic.access.domain.RolePolicy}
     * rather than switching here, so the route lives beside the rest of what the
     * role may do (FR-OOP-04).</p>
     */
    public static String homeFor(Role role) {
        return RolePolicy.of(role).homePath();
    }

    /**
     * @return the name of the role that owns {@code path}, or null when no role
     *         does - the JSON API, shared pages, static assets.
     */
    private static String roleOwning(String path) {
        for (Role role : Role.values()) {
            if (path.startsWith(RolePolicy.of(role).ownedPrefix())) {
                return role.name();
            }
        }
        return null;
    }

    /**
     * Refuse a signed-in user a page belonging to another role.
     *
     * <p>403 and not a redirect to their own home: a redirect would suggest the
     * address was wrong, when the truth is that it exists and is not theirs.</p>
     */
    private static void refuse(HttpServletRequest request, HttpServletResponse response,
                               String path, ClinicPrincipal principal) throws IOException, ServletException {
        LOG.log(java.util.logging.Level.WARNING,
                "access_refused path={0} role={1} uid={2}",
                new Object[] { path, principal.role(), principal.uid() });
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (path.startsWith("/api/")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(Json.write(Map.of(
                    "errorCode", "forbidden",
                    "message", "Your role (" + principal.role() + ") does not permit this action.")));
            return;
        }
        request.setAttribute("attemptedPath", path);
        request.getRequestDispatcher("/WEB-INF/jsp/shared/forbidden.jsp").forward(request, response);
    }

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(AuthenticationFilter.class.getName());

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
