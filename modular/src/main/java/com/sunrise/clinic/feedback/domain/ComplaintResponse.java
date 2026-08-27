package com.sunrise.clinic.feedback.domain;

import java.time.Instant;

/**
 * A complaint as the patient who raised it, or the administrator reviewing it, sees it.
 *
 * <p>There is one shape, not two, and that is deliberate: the only two audiences are the
 * patient and the administrator, and both should see the same account and the same
 * resolution. What differs is who can obtain one at all.</p>
 *
 * <p>It carries the dentist's <b>name</b> for the administrator's list (FR-ADM-54) and
 * nothing clinical - no diagnosis, no medical note - even where the complaint is about the
 * treatment (FR-ADM-58). An administrator reviewing conduct is not the patient's dentist.</p>
 */
public record ComplaintResponse(String id,
                                String patientName,
                                String dentistName,
                                String appointmentNo,
                                ComplaintCategory category,
                                String categoryLabel,
                                String detail,
                                ComplaintStatus status,
                                String statusLabel,
                                Instant submittedAt,
                                String resolution,
                                Instant resolvedAt) {

    public static ComplaintResponse of(Complaint complaint, String patientName, String dentistName) {
        return new ComplaintResponse(complaint.getId(), patientName, dentistName,
                complaint.getAppointmentNo(),
                complaint.getCategory(),
                complaint.getCategory() == null ? "" : complaint.getCategory().label(),
                complaint.getDetail(),
                complaint.getStatus(),
                complaint.getStatus() == null ? "" : complaint.getStatus().label(),
                complaint.getSubmittedAt(),
                complaint.getResolution(),
                complaint.getResolvedAt());
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }
}
