package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.RolePolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 2. In the same package as the servlets so it can reach the
 * protected hooks without reflection.
 *
 * <p>Nothing here touches a container. What is worth asserting about the four
 * portals is not that a request produces a response — that was verified against
 * the deployed application — but that the Template Method holds: the sequence
 * lives in the superclass, each subclass supplies only what distinguishes it,
 * and the four together cover the four roles exactly once (FR-OOP-11).</p>
 */
class AbstractLoginServletTest {

    private static final List<AbstractLoginServlet> PORTALS = List.of(
            new PatientLoginServlet(),
            new ReceptionLoginServlet(),
            new DentistLoginServlet(),
            new AdminLoginServlet());

    @Test
    void thereIsOnePortalPerRole() {
        Set<Role> covered = new HashSet<>();
        for (AbstractLoginServlet portal : PORTALS) {
            assertTrue(covered.add(portal.acceptedRole()),
                    portal.acceptedRole() + " is accepted by two portals");
        }
        assertEquals(Set.of(Role.values()), covered, "every role needs a portal, and no more");
    }

    @Test
    void eachPortalRendersItsOwnView() {
        Set<String> views = new HashSet<>();
        for (AbstractLoginServlet portal : PORTALS) {
            String view = portal.viewName();
            assertTrue(views.add(view), view + " is rendered by two portals");
            assertFalse(view.startsWith("/"), "viewName is relative to /WEB-INF/jsp/: " + view);
            assertFalse(view.endsWith(".jsp"), "viewName carries no extension: " + view);
        }
    }

    @Test
    void aPortalsViewNameMatchesTheRoleItAdmits() {
        // access/login-dentist for DENTIST. A copy-paste that left the wrong view
        // name would otherwise show the patient page on the dentist's portal.
        for (AbstractLoginServlet portal : PORTALS) {
            assertEquals("access/login-" + shortName(portal.acceptedRole()), portal.viewName());
        }
    }

    @Test
    void onlyThePatientPortalOffersSelfRegistration() {
        // Patients register themselves; staff accounts come from an administrator.
        // A "create an account" link on the admin portal would be an open door.
        for (AbstractLoginServlet portal : PORTALS) {
            assertEquals(portal.acceptedRole() == Role.PATIENT,
                    portal.allowsSelfRegistration(),
                    portal.getClass().getSimpleName());
        }
    }

    @Test
    void everyPortalMatchesItsRolesDeclaredLoginPath() {
        // RolePolicy.loginPath() is what the portal chooser and the redirects use.
        // If it disagreed with web.xml's mapping for this servlet, sign-in would
        // send people to a page that does not admit them.
        for (AbstractLoginServlet portal : PORTALS) {
            Role role = portal.acceptedRole();
            assertEquals("/login/" + shortName(role), RolePolicy.of(role).loginPath());
        }
    }

    @Test
    void theSequenceIsFinalSoNoSubclassCanReorderIt() {
        // The point of the Template Method: a subclass supplies the role and the
        // view, never its own version of the authentication sequence. If doPost
        // stopped being final, a fifth portal could skip the password check.
        for (String name : List.of("doGet", "doPost")) {
            assertTrue(Modifier.isFinal(findDeclared(AbstractLoginServlet.class, name).getModifiers()),
                    name + " must be final");
        }
    }

    @Test
    void noSubclassOverridesTheSequence() {
        List<String> offenders = new ArrayList<>();
        for (AbstractLoginServlet portal : PORTALS) {
            for (Method method : portal.getClass().getDeclaredMethods()) {
                if (method.getName().equals("doGet") || method.getName().equals("doPost")) {
                    offenders.add(portal.getClass().getSimpleName() + "." + method.getName());
                }
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    void eachSubclassStaysSmall() {
        // Four subclasses of two short methods each is the whole benefit. If one
        // grows its own logic, the shared sequence has started to leak back out.
        for (AbstractLoginServlet portal : PORTALS) {
            long declared = portal.getClass().getDeclaredMethods().length;
            assertTrue(declared <= 3,
                    portal.getClass().getSimpleName() + " declares " + declared + " methods");
        }
    }

    @Test
    void theSignInFormPostsToAPathTheServletSupplies() throws Exception {
        // A guard against the one defect in this step that no test without a
        // browser could see. The form's action was
        // ${pageContext.request.servletPath}, and because the view is reached by a
        // forward that evaluates to the view's own path - so the browser posted to
        // /WEB-INF/jsp/access/login-dentist.jsp and got a 404, while the same
        // credentials over curl worked, because curl posts to the URL and never
        // reads the form. Cheap to assert, and it fails loudly if reintroduced.
        String form = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp/WEB-INF/jsp/access/login-form.jspf"));

        assertTrue(form.contains("action=\"${ctx}${loginPath}\""),
                "the form must post to the path the servlet supplies");
        assertFalse(form.contains("action=\"${ctx}${pageContext.request.servletPath}\""),
                "servletPath is the view's own path inside a forward");
    }

    private static String shortName(Role role) {
        return switch (role) {
            case PATIENT -> "patient";
            case RECEPTIONIST -> "reception";
            case DENTIST -> "dentist";
            case ADMIN -> "admin";
        };
    }

    private static Method findDeclared(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError(type.getSimpleName() + " declares no " + name);
    }
}
