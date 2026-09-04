package com.sunrise.clinic.reporting;

import com.sunrise.clinic.access.data.InMemoryUserRepository;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AuthService;
import com.sunrise.clinic.access.service.LoginAttemptService;
import com.sunrise.clinic.access.service.UserAccountFactory;
import com.sunrise.clinic.platform.audit.InMemoryAuditRepository;
import com.sunrise.clinic.platform.data.InMemoryPersonSequenceRepository;
import com.sunrise.clinic.platform.service.PersonNumberGenerator;
import com.sunrise.clinic.reporting.service.AccountAdminService;
import com.sunrise.clinic.scheduling.data.InMemoryDentistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** GAP-ADM-11: every new account and dentist gets a quotable number. */
class AccountNumbersTest {

    private static final ClinicPrincipal ADMIN =
            new ClinicPrincipal("u-admin", "Anoma Fernando", Role.ADMIN);

    private InMemoryUserRepository users;
    private InMemoryDentistRepository dentists;
    private AccountAdminService accounts;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        dentists = new InMemoryDentistRepository();
        PersonNumberGenerator numbers =
                new PersonNumberGenerator(new InMemoryPersonSequenceRepository());
        accounts = new AccountAdminService(users,
                new UserAccountFactory(users, numbers),
                new AuthService(users, new LoginAttemptService(users)),
                dentists, new InMemoryAuditRepository(), numbers);
    }

    @Test
    void newDentistGetsANumberedId() {
        AccountAdminService.NewAccount created = accounts.createStaff(ADMIN,
                "newdoc@example.lk", "Dr. New Doc", Role.DENTIST, "General Dentistry",
                new BigDecimal("1500.00"), "new.doc");

        assertTrue(created.account().accountNumber().matches("\\d{6}DEN\\d{4}"),
                "dentist accounts get YYMMDDDENNNNN: " + created.account().accountNumber());
        assertTrue(dentists.findAll().get(0).getId().matches("\\d{6}DEN\\d{4}"),
                "dentist records get YYMMDDDENNNNN: " + dentists.findAll().get(0).getId());
    }

    @Test
    void newReceptionistGetsANumberedAccount() {
        AccountAdminService.NewAccount created = accounts.createStaff(ADMIN,
                "desk@example.lk", "Desk Person", Role.RECEPTIONIST, null, null, "desk.person");

        assertTrue(created.account().accountNumber().matches("\\d{6}REC\\d{4}"),
                "reception accounts get YYMMDDRECNNNN: " + created.account().accountNumber());
    }
}
