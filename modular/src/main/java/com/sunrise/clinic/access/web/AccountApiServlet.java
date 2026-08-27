package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.json.Json;
import com.sunrise.clinic.platform.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * {@code POST /api/accounts} — the only way a staff account comes into
 * existence.
 *
 * <p>Separate from {@code POST /api/auth/register} on purpose. That endpoint is
 * patient self-registration and hardcodes {@code PATIENT}, so a {@code role}
 * field in its body has nothing to bind to. This one takes an explicit role and
 * requires {@link Action#MANAGE_ACCOUNTS}, so the two paths cannot be confused
 * for each other (FR-ADM-20, FR-PAT-03).</p>
 *
 * <p>The dentist profile row that FR-ADM-21 asks for arrives with the
 * {@code scheduling} module in step 3.</p>
 */
public class AccountApiServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(response, () -> {
            AccessControl.require(currentUser(request), Action.MANAGE_ACCOUNTS);

            Map<String, Object> body = readBody(request);
            Role role = parseRole(Json.string(body, "role"));

            UserAccount created = app().accountFactory().createStaff(
                    required(body, "email"),
                    required(body, "password"),
                    required(body, "displayName"),
                    role);

            writeJson(response, HttpServletResponse.SC_CREATED, Map.of(
                    "uid", created.getUid(),
                    "email", created.getEmail(),
                    "role", created.getRole()));
        });
    }

    private static Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "role must be one of RECEPTIONIST, DENTIST, ADMIN");
        }
    }

    private static String required(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
