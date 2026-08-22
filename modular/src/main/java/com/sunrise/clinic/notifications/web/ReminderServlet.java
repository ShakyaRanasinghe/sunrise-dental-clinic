package com.sunrise.clinic.notifications.web;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.notifications.service.ReminderService;
import com.sunrise.clinic.platform.web.BaseServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Runs the reminder sweep — FR-NOT-02.
 *
 * <pre>
 *   POST /api/reminders/run     the sweep
 *   GET  /api/reminders/run     what a sweep would do, without doing it
 * </pre>
 *
 * <h2>How this is authorised, and why it needs saying</h2>
 *
 * <p>An endpoint that sends messages to patients is a spam gun if anybody can call it, and the
 * caller is cron, which cannot sign in. So it accepts <b>either</b> of two things and nothing
 * else:</p>
 *
 * <ul>
 *   <li>a signed-in administrator — which is how a person triggers it while watching, and is
 *       checked with {@code READ_REPORTS} rather than a role, like everything else here;</li>
 *   <li>a shared token in {@code X-Clinic-Token}, matching {@code clinic.reminders.token}.</li>
 * </ul>
 *
 * <p><b>With no token configured, only the administrator path works.</b> That is the safe
 * default: a clinic that has not set one up cannot be swept by a stranger, and the failure is a
 * 403 that says the token is not configured rather than a silent success.</p>
 *
 * <p>The comparison is constant-time. A token compared with {@code equals} leaks its length and
 * its leading characters to somebody timing the responses, and this one is in a cron entry that
 * will not be rotated for years.</p>
 *
 * <p>It is in {@code AuthenticationFilter}'s public list for the same reason {@code /api/auth/}
 * is — the filter would otherwise turn cron away before this class could look at the token. The
 * authorising is done here instead, and doing it here means it is done: there is no path through
 * this servlet that skips it.</p>
 */
public class ReminderServlet extends BaseServlet {

    private static final String TOKEN_HEADER = "X-Clinic-Token";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(response, () -> {
            requirePermission(request);
            ReminderService.Sweep sweep = app().reminderService().run();
            writeJson(response, Map.of(
                    "remindingAbout", sweep.date().toString(),
                    "due", sweep.due(),
                    "reminded", sweep.reminded(),
                    "skipped", sweep.skipped(),
                    "failed", sweep.failed()));
        });
    }

    /**
     * What a sweep would do, without doing it.
     *
     * <p>So somebody setting up the cron entry can check the endpoint is reachable and correctly
     * authorised without sending anything to a patient.</p>
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(response, () -> {
            requirePermission(request);
            writeJson(response, Map.of(
                    "remindingAbout", app().reminderService().remindingAbout().toString(),
                    "note", "POST to this address to run the sweep."));
        });
    }

    /**
     * @throws AccessControl.AccessDeniedException unless the caller is an administrator or
     *         presents the configured token
     */
    private void requirePermission(HttpServletRequest request) {
        ClinicPrincipal caller = currentUser(request);
        if (caller != null) {
            AccessControl.require(caller, Action.READ_REPORTS);
            return;
        }

        String configured = app().config().get("clinic.reminders.token", "");
        if (configured.isBlank()) {
            throw new AccessControl.AccessDeniedException(
                    "No reminder token is configured, so this can only be run by a signed-in"
                            + " administrator. Set clinic.reminders.token to run it from cron.");
        }
        String presented = request.getHeader(TOKEN_HEADER);
        if (presented == null || !constantTimeEquals(configured, presented)) {
            throw new AccessControl.AccessDeniedException(
                    "That is not the reminder token.");
        }
    }

    /**
     * Compares without leaking how much of the token was right.
     *
     * <p>{@code equals} returns as soon as two characters differ, so the time it takes reveals
     * the length and then the leading characters, one request at a time. It matters here more
     * than it usually does because this token sits in a cron entry and will not be rotated.</p>
     */
    private static boolean constantTimeEquals(String expected, String presented) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
