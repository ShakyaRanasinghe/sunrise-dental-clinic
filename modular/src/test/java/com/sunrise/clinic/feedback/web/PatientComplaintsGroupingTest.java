package com.sunrise.clinic.feedback.web;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The grouping that makes the "Raise a concern" page list one collapsible form per
 * dentist instead of one form per appointment (GAP-PAT-18).
 */
class PatientComplaintsGroupingTest {

    private AppointmentResponse visit(String no, String dentistId, String dentistName,
                                      String date, String treatment) {
        return new AppointmentResponse(no, "p-nimal", "Nimal", dentistId, dentistName,
                "slot", "t-1", treatment, LocalDate.parse(date), LocalTime.of(10, 0),
                AppointmentStatus.COMPLETED);
    }

    private static List<PatientComplaintsServlet.DentistVisits> group(
            List<AppointmentResponse> appointments) throws Exception {
        Method method = PatientComplaintsServlet.class.getDeclaredMethod(
                "groupByDentist", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PatientComplaintsServlet.DentistVisits> result =
                (List<PatientComplaintsServlet.DentistVisits>) method.invoke(null, appointments);
        return result;
    }

    @Test
    void groupsVisitsByDentist() throws Exception {
        List<AppointmentResponse> appointments = List.of(
                visit("APT-1", "d-silva", "Dr Silva", "2026-08-01", "Check-up"),
                visit("APT-2", "d-jaya", "Dr Jayasuriya", "2026-08-02", "Scaling"),
                visit("APT-3", "d-silva", "Dr Silva", "2026-08-20", "Filling"));

        List<PatientComplaintsServlet.DentistVisits> grouped = group(appointments);

        assertEquals(2, grouped.size());
        PatientComplaintsServlet.DentistVisits silva = grouped.get(0);
        assertEquals("d-silva", silva.dentistId());
        assertEquals("Dr Silva", silva.dentistName());
        assertEquals(2, silva.visits().size());
        assertEquals("APT-3", silva.visits().get(0).appointmentNo(),
                "visits sorted newest first");
        assertEquals("APT-1", silva.visits().get(1).appointmentNo());
        assertNotNull(grouped.stream()
                .filter(v -> v.dentistId().equals("d-jaya")).findFirst().orElse(null));
    }
}
