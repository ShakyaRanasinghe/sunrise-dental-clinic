package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Role;

/** The ADMIN portal. Clinic reports and accounts. */
public class AdminLoginServlet extends AbstractLoginServlet {

    @Override
    protected Role acceptedRole() {
        return Role.ADMIN;
    }

    @Override
    protected String viewName() {
        return "access/login-admin";
    }

    @Override
    protected boolean auditFailedAttempts() {
        return true;
    }
}
