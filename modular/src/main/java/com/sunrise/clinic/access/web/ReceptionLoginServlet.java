package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Role;

/** The RECEPTIONIST portal. Front desk access. */
public class ReceptionLoginServlet extends AbstractLoginServlet {

    @Override
    protected Role acceptedRole() {
        return Role.RECEPTIONIST;
    }

    @Override
    protected String viewName() {
        return "access/login-reception";
    }
}
