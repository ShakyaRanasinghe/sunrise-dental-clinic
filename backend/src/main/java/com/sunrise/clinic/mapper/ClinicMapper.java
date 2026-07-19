package com.sunrise.clinic.mapper;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.dto.AppointmentDetailResponse;
import com.sunrise.clinic.dto.AppointmentResponse;
import com.sunrise.clinic.dto.BillResponse;
import com.sunrise.clinic.dto.SlotResponse;
import org.springframework.stereotype.Component;

/** Maps domain entities to the appropriate response DTOs (DTO pattern). */
@Component
public class ClinicMapper {

    public SlotResponse toSlotResponse(Slot s) {
        return new SlotResponse(s.getId(), s.getDentistId(), s.getDate(),
                s.getStartTime(), s.getDurationMinutes(), s.getStatus());
    }

    /** Non-clinical view (no diagnosis) — for Receptionist/Admin. */
    public AppointmentResponse toAppointmentResponse(Appointment a) {
        return new AppointmentResponse(a.getAppointmentNo(), a.getPatientId(), a.getDentistId(),
                a.getSlotId(), a.getTreatmentId(), a.getDate(), a.getTime(), a.getStatus());
    }

    /** Clinical view (includes diagnosis) — for the treating Dentist or the Patient. */
    public AppointmentDetailResponse toAppointmentDetail(Appointment a) {
        return new AppointmentDetailResponse(a.getAppointmentNo(), a.getPatientId(), a.getDentistId(),
                a.getSlotId(), a.getTreatmentId(), a.getDate(), a.getTime(), a.getStatus(), a.getDiagnosis());
    }

    public BillResponse toBillResponse(Bill b) {
        return new BillResponse(b.getId(), b.getAppointmentNo(), b.getConsultationFee(),
                b.getTreatmentCost(), b.getServiceCharge(), b.getDiscount(), b.getTax(), b.getTotal());
    }
}
