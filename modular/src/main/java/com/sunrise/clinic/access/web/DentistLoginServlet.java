package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Role;

/** The DENTIST portal. Your schedule and treatment records. */
public class DentistLoginServlet extends AbstractLoginServlet {

    @Override
    protected Role acceptedRole() {
        return Role.DENTIST;
    }

    @Override
    protected String viewName() {
        return "access/login-dentist";
    }
}
