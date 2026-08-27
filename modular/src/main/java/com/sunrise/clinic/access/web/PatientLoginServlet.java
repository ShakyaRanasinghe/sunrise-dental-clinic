package com.sunrise.clinic.access.web;

import com.sunrise.clinic.access.domain.Role;

/** The PATIENT portal. Sign in to manage your appointments. */
public class PatientLoginServlet extends AbstractLoginServlet {

    @Override
    protected Role acceptedRole() {
        return Role.PATIENT;
    }

    @Override
    protected String viewName() {
        return "access/login-patient";
    }

    @Override
    protected boolean allowsSelfRegistration() {
        return true;
    }
}
