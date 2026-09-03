package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.RolePolicy;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.ReleaseInfo;
import com.sunrise.clinic.platform.di.AppContext;
import com.sunrise.clinic.platform.di.ClinicServletContext;
import com.sunrise.clinic.platform.json.Json;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
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
 * <p>It also enforces one <em>coarse</em> role rule: a path belonging to a role area may
 * only be entered by a role permitted there, so a signed-in patient cannot open
 * {@code /admin/reports}. The prefixes come from {@link RolePolicy#enterablePrefixes()},
 * derived from each role's own home path, so this is not a second list of URL patterns to
 * drift out of step with the code.</p>
 *
 * <p><b>Fine-grained</b> checks stay in the servlets, through {@link AccessControl},
 * next to the operation they protect - whether this receptionist may cancel <em>this</em>
 * appointment is not a question about a URL. The two are deliberately layered: the
 * filter is a structural guarantee that holds for a servlet whose author forgot to
 * call {@code require}, and the servlet check is the one that knows the specifics.</p>
 *
 * <p>This was found by testing rather than reading: at step 2 a signed-in patient
 * could load all four role landing pages with a 200, because the temporary
 * the temporary stub landing page checked nothing. Relying on every one of thirty-two
 * servlets to remember is the discipline-not-construction problem this codebase
 * avoids elsewhere.</p>
 */
public class AuthenticationFilter implements Filter {

    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) {
        this.servletContext = filterConfig.getServletContext();
    }

    /** Paths reachable without signing in. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/login",          // the four login portals beneath it
            "/logout",
            "/register",
            "/help",
            "/staff",          // the staff role chooser, itself a public landing page
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

        decorateFooter(request, path);

        if (principal != null) {
            if (isForbiddenPrefix(path, principal)) {
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

        // A session that expires leaves no trace of who held it, so the page they
        // were on is the only clue to their role. Send them to the login of the area
        // they were in — or, for a patient, back to the public home page the patient
        // portal is entered from.
        response.sendRedirect(request.getContextPath() + anonymousLanding(path));
    }

    /**
     * Where an anonymous browser that reached a protected page should be sent.
     *
     * <p>Each role owns exactly one URL prefix ({@link RolePolicy#ownedPrefix()},
     * derived from its home path), so the requested path picks out the area the
     * caller was in and this returns that role's own login page. Patients are the
     * exception: the patient portal is entered from the public home page, so a
     * patient whose session lapsed is returned to {@code /} rather than the
     * password gate. An unfamiliar path (none should survive to here) falls back
     * to the public home page too.</p>
     */
    static String anonymousLanding(String path) {
        for (Role role : Role.values()) {
            RolePolicy policy = RolePolicy.of(role);
            if (path.startsWith(policy.ownedPrefix())) {
                return role == Role.PATIENT ? "/" : policy.loginPath();
            }
        }
        return "/";
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

    /**
     * Give the shared footer the clinic identity and release number to draw on.
     *
     * <p>Every page includes {@code footer.jspf}, so the clinic's name, address,
     * phone, email and release version have to be on every page that renders one.
     * This filter is the one pass every such request makes — the "define once"
     * answer to a piece of data every view needs. Static assets and the JSON API
     * render nothing, so they are skipped rather than charged four lookups each.</p>
     *
     * <p>A database failure here must not take a page down before the servlet has
     * even run: the footer is decoration, so on failure the page simply renders
     * without the contact block and version.</p>
     */
    private void decorateFooter(HttpServletRequest request, String path) {
        if (path.startsWith("/api/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || "/favicon.ico".equals(path)) {
            return;
        }
        try {
            AppContext appContext = ClinicServletContext.get(servletContext);
            request.setAttribute("clinicName", appContext.clinicIdentity().get("clinic.name"));
            request.setAttribute("clinicPhone", appContext.clinicIdentity().get("clinic.phone"));
            request.setAttribute("clinicEmail", appContext.clinicIdentity().get("clinic.email"));
            request.setAttribute("clinicAddress", appContext.clinicIdentity().get("clinic.address"));
            request.setAttribute("releaseVersion", ReleaseInfo.version());
        } catch (RuntimeException e) {
            LOG.log(java.util.logging.Level.WARNING, "footer_context_unavailable", e);
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
     * @return true if {@code path} belongs to a role area this caller may not enter
     *
     * <p>A path no role owns - the JSON API, the shared pages, static assets - is not
     * this check's business and passes through.</p>
     */
    private static boolean isForbiddenPrefix(String path, ClinicPrincipal principal) {
        boolean ownedBySomeone = RolePolicy.allOwnedPrefixes().stream().anyMatch(path::startsWith);
        if (!ownedBySomeone) {
            return false;
        }
        return principal.policy().enterablePrefixes().stream().noneMatch(path::startsWith);
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
