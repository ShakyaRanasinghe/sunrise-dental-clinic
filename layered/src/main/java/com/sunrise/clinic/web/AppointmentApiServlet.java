package com.sunrise.clinic.web;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.json.Json;
import com.sunrise.clinic.security.AccessControl;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Everything that hangs off {@code /api/appointments}.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>{@code POST   /api/appointments}</td><td>book an appointment</td></tr>
 *   <tr><td>{@code GET    /api/appointments/{no}}</td><td>fetch one</td></tr>
 *   <tr><td>{@code POST   /api/appointments/{no}/cancel}</td><td>cancel it</td></tr>
 *   <tr><td>{@code POST   /api/appointments/{no}/complete}</td><td>record the diagnosis</td></tr>
 *   <tr><td>{@code POST   /api/appointments/{no}/bill}</td><td>generate the bill</td></tr>
 *   <tr><td>{@code GET    /api/appointments/{no}/bill}</td><td>fetch the bill</td></tr>
 * </table>
 *
 * <p>A servlet is mapped to a URL prefix rather than to individual routes, so the
 * billing endpoints live here too: {@code /api/appointments/*} can only be served
 * by one servlet. The sub-path is dispatched explicitly below, which is the work a
 * request-mapping annotation used to do.</p>
 */
public class AppointmentApiServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.isEmpty()) {
                book(request, response);
                return;
            }
            if (path.size() != 2) {
                throw new IllegalArgumentException("Unknown endpoint");
            }
            String appointmentNo = path.get(0);
            switch (path.get(1)) {
                case "cancel" -> cancel(request, response, appointmentNo);
                case "complete" -> complete(request, response, appointmentNo);
                case "bill" -> generateBill(request, response, appointmentNo);
                default -> throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            List<String> path = pathParts(request);
            if (path.size() == 1) {
                get(request, response, path.get(0));
            } else if (path.size() == 2 && "bill".equals(path.get(1))) {
                getBill(response, path.get(0));
            } else {
                throw new IllegalArgumentException("Unknown endpoint");
            }
        });
    }

    // ------------------------------------------------------------------

    private void book(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClinicPrincipal user = currentUser(request);
        AccessControl.require(user, Role.PATIENT, Role.RECEPTIONIST, Role.ADMIN);

        Map<String, Object> body = readBody(request);
        String slotId = required(body, "slotId");
        String treatmentId = required(body, "treatmentId");
        String patientId = Json.string(body, "patientId");

        // A patient booking for themselves does not send a patientId — resolve it
        // from their own profile so they cannot book in someone else's name.
        if (user.role() == Role.PATIENT) {
            patientId = app().patients().findByUserUid(user.uid())
                    .map(Patient::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No patient profile for user " + user.uid()));
        }
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException(
                    "patientId is required when booking on behalf of a patient");
        }

        Appointment appointment = app().appointmentService()
                .book(patientId, slotId, treatmentId, user.uid(), user.role());
        writeJson(response, HttpServletResponse.SC_CREATED,
                app().mapper().toAppointmentResponse(appointment));
    }

    /** Returns the clinical view (with diagnosis) only to the treating dentist or the patient. */
    private void get(HttpServletRequest request, HttpServletResponse response, String appointmentNo)
            throws IOException {
        ClinicPrincipal user = currentUser(request);
        Appointment appointment = app().appointmentService().findByNo(appointmentNo);

        if (app().clinicAccess().canViewClinical(appointment, user)) {
            writeJson(response, app().mapper().toAppointmentDetail(appointment));
        } else {
            writeJson(response, app().mapper().toAppointmentResponse(appointment));
        }
    }

    private void cancel(HttpServletRequest request, HttpServletResponse response, String appointmentNo)
            throws IOException {
        ClinicPrincipal user = currentUser(request);
        AccessControl.require(user, Role.PATIENT, Role.RECEPTIONIST, Role.ADMIN);

        // A patient may only cancel their own appointment.
        if (user.role() == Role.PATIENT) {
            Appointment existing = app().appointmentService().findByNo(appointmentNo);
            String ownPatientId = app().patients().findByUserUid(user.uid())
                    .map(Patient::getId).orElse(null);
            if (!existing.getPatientId().equals(ownPatientId)) {
                throw new AccessControl.AccessDeniedException(
                        "You can only cancel your own appointments.");
            }
        }

        writeJson(response, app().mapper()
                .toAppointmentResponse(app().appointmentService().cancel(appointmentNo)));
    }

    private void complete(HttpServletRequest request, HttpServletResponse response, String appointmentNo)
            throws IOException {
        AccessControl.require(currentUser(request), Role.DENTIST);
        Map<String, Object> body = readBody(request);
        Appointment appointment = app().appointmentService()
                .complete(appointmentNo, Json.string(body, "diagnosis"));
        writeJson(response, app().mapper().toAppointmentDetail(appointment));
    }

    private void generateBill(HttpServletRequest request, HttpServletResponse response,
                              String appointmentNo) throws IOException {
        ClinicPrincipal user = currentUser(request);
        AccessControl.require(user, Role.RECEPTIONIST, Role.ADMIN);
        Bill bill = app().billingService().generateBill(appointmentNo, user.uid());
        writeJson(response, HttpServletResponse.SC_CREATED, app().mapper().toBillResponse(bill));
    }

    private void getBill(HttpServletResponse response, String appointmentNo) throws IOException {
        Bill bill = app().bills().findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No bill for appointment " + appointmentNo));
        writeJson(response, app().mapper().toBillResponse(bill));
    }

    private static String required(Map<String, Object> body, String field) {
        String value = Json.string(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
