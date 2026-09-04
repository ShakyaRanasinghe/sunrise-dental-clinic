package com.sunrise.clinic.feedback.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.appointments.service.AppointmentService;
import com.sunrise.clinic.appointments.service.ClinicAccess;
import com.sunrise.clinic.feedback.data.ComplaintRepository;
import com.sunrise.clinic.feedback.domain.Complaint;
import com.sunrise.clinic.feedback.domain.ComplaintCategory;
import com.sunrise.clinic.feedback.domain.ComplaintResponse;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.audit.AuditEvent;
import com.sunrise.clinic.platform.audit.AuditRepository;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Complaints about a dentist - FR-CMP-01 to FR-CMP-12, FR-ADM-50 to FR-ADM-58.
 *
 * <h2>Who sees a complaint</h2>
 *
 * <p>The patient who raised it, and the administrator. <b>Nobody else, ever.</b></p>
 *
 * <p>The dentist named in it never sees it - not the complaint, not its existence, not a
 * count that would let them infer it (FR-CMP-08). This is not enforced by leaving a link off
 * a page: there is no method here a dentist can call successfully, and no response shape that
 * reaches them. A receptionist likewise (FR-CMP-09).</p>
 *
 * <p>Raising one changes nothing else. It does not affect booking, and it appears on no
 * screen the patient's dentist can reach (FR-CMP-10) - because a complaints process that
 * costs the complainant their next appointment is a process nobody uses.</p>
 *
 * <p>Every read is audited, naming who read it (FR-CMP-11). Reading a person's account of
 * something that upset them is itself an act worth recording.</p>
 */
public class ComplaintService {

    private static final Logger log = Logger.getLogger(ComplaintService.class.getName());

    private static final int MIN_DETAIL = 20;
    private static final int MAX_DETAIL = 5000;

    private final ComplaintRepository complaints;
    private final AppointmentService appointments;
    private final ClinicAccess clinicAccess;
    private final ReferenceService reference;
    private final AuditRepository audit;

    public ComplaintService(ComplaintRepository complaints, AppointmentService appointments,
                            ClinicAccess clinicAccess, ReferenceService reference,
                            AuditRepository audit) {
        this.complaints = complaints;
        this.appointments = appointments;
        this.clinicAccess = clinicAccess;
        this.reference = reference;
        this.audit = audit;
    }

    // --- the patient's side -------------------------------------------

    /**
     * Raise a complaint - FR-CMP-01.
     *
     * <p>The dentist must be one the patient has actually been treated by: a complaints
     * channel open against anybody is a channel for abuse rather than for concerns.</p>
     *
     * @param appointmentNo optional - the visit it concerns, chosen from the patient's own
     *                      appointments (FR-CMP-03)
     */
    public ComplaintResponse raise(ClinicPrincipal caller, String dentistId, String appointmentNo,
                                   ComplaintCategory category, String detail) {
        AccessControl.require(caller, Action.RAISE_CONCERN);
        String patientId = requireOwnPatientId(caller);

        // Names a real dentist, and one this patient has seen.
        reference.requireDentist(dentistId);
        List<AppointmentResponse> own = appointments.forSelf(caller);
        if (own.stream().noneMatch(a -> dentistId.equals(a.dentistId()))) {
            throw new IllegalArgumentException(
                    "You can only raise a concern about a dentist who has treated you.");
        }
        if (appointmentNo != null && !appointmentNo.isBlank()
                && own.stream().noneMatch(a -> appointmentNo.equals(a.appointmentNo()))) {
            throw new IllegalArgumentException("That is not one of your appointments.");
        }

        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .dentistId(dentistId)
                .appointmentNo(appointmentNo == null || appointmentNo.isBlank()
                        ? null : appointmentNo)
                .category(requireCategory(category))
                .detail(requireDetail(detail))
                .status(ComplaintStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();
        complaints.save(complaint);

        // The detail is never logged: it is a patient's account of something distressing.
        log.log(Level.INFO, "complaint_raised id={0} category={1}",
                new Object[] { complaint.getId(), complaint.getCategory() });
        return describe(complaint);
    }

    /**
     * Raise a general concern about the clinic (GAP-PAT-32) — no dentist, no visit,
     * raisable before any appointment.
     *
     * <p>Stored and shown anonymously: patient and dentist are NULL, so neither the
     * administrator's queue nor any later lookup can tie it to whoever wrote it. The
     * caller must still be a signed-in patient (the page itself is the gate against
     * drive-by abuse); anonymity is in what is stored, not in who may write.</p>
     */
    public ComplaintResponse raiseGeneral(ClinicPrincipal caller,
                                          ComplaintCategory category, String detail) {
        AccessControl.require(caller, Action.RAISE_CONCERN);

        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID().toString())
                .patientId(null)
                .dentistId(null)
                .appointmentNo(null)
                .category(requireCategory(category))
                .detail(requireDetail(detail))
                .status(ComplaintStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();
        complaints.save(complaint);

        log.log(Level.INFO, "general_concern_raised id={0} category={1}",
                new Object[] { complaint.getId(), complaint.getCategory() });
        return describe(complaint);
    }

    /** What this patient has raised, and where each stands - FR-CMP-04. */
    public List<ComplaintResponse> own(ClinicPrincipal caller) {
        String patientId = requireOwnPatientId(caller);
        return complaints.findByPatientId(patientId).stream().map(this::describe).toList();
    }

    // --- the administrator's side --------------------------------------

    /** The administrator's list - FR-ADM-50, open ones first. */
    public List<ComplaintResponse> search(ClinicPrincipal caller, ComplaintStatus status,
                                          String dentistId, LocalDate from, LocalDate to) {
        AccessControl.require(caller, Action.REVIEW_CONCERNS);
        List<Complaint> found = complaints.search(status, dentistId, from, to);
        // FR-CMP-11: reading is audited, and reading a list is reading each of them.
        recordRead(caller, found.size() + " complaints");
        return found.stream().map(this::describe).toList();
    }

    /** One complaint, with the appointment it concerns - FR-ADM-54. */
    public ComplaintResponse read(ClinicPrincipal caller, String complaintId) {
        AccessControl.require(caller, Action.REVIEW_CONCERNS);
        Complaint complaint = require(complaintId);
        recordRead(caller, complaintId);
        return describe(complaint);
    }

    /** Pick it up - FR-ADM-52. */
    public ComplaintResponse beginReview(ClinicPrincipal caller, String complaintId) {
        AccessControl.require(caller, Action.REVIEW_CONCERNS);
        Complaint complaint = require(complaintId);
        complaint.beginReview(caller.uid());
        complaints.save(complaint);
        record(caller, "COMPLAINT_UNDER_REVIEW", complaintId);
        return describe(complaint);
    }

    /**
     * Close it, with a written resolution - FR-ADM-52, FR-ADM-53.
     *
     * <p>The resolution is stored beside the patient's account, never over it: nothing here
     * or in {@code ComplaintDao} can write {@code detail} after the insert (FR-ADM-55).</p>
     */
    public ComplaintResponse close(ClinicPrincipal caller, String complaintId,
                                   ComplaintStatus outcome, String resolution) {
        AccessControl.require(caller, Action.REVIEW_CONCERNS);
        Complaint complaint = require(complaintId);
        complaint.close(outcome, caller.uid(), resolution);
        complaints.save(complaint);
        record(caller, "COMPLAINT_" + outcome, complaintId);
        log.log(Level.INFO, "complaint_closed id={0} outcome={1} by={2}",
                new Object[] { complaintId, outcome, caller.uid() });
        return describe(complaint);
    }

    /**
     * How many complaints name each dentist - FR-ADM-57.
     *
     * <p>Administrator only, and a count rather than a rate. Complaints per appointment
     * would turn a handful of concerns into a performance metric, which is not what they
     * are.</p>
     */
    public Map<String, Integer> countByDentist(ClinicPrincipal caller) {
        AccessControl.require(caller, Action.REVIEW_CONCERNS);
        return complaints.countByDentist();
    }

    // --- helpers ------------------------------------------------------

    private Complaint require(String complaintId) {
        return complaints.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No complaint " + complaintId));
    }

    private String requireOwnPatientId(ClinicPrincipal caller) {
        if (caller == null || caller.role() != Role.PATIENT) {
            throw new AccessControl.AccessDeniedException(
                    "Only a patient can raise a concern about their own care.");
        }
        return clinicAccess.patientFor(caller).map(Patient::getId).orElseThrow(() ->
                new ResourceNotFoundException("Your account has no patient record."));
    }

    private ComplaintResponse describe(Complaint complaint) {
        // GAP-PAT-32: anonymous concerns carry neither identity.
        String patientName = complaint.getPatientId() == null ? null
                : clinicAccess.patientById(complaint.getPatientId())
                        .map(Patient::getName).orElse(null);
        String dentistName = complaint.getDentistId() == null ? null
                : reference.requireDentist(complaint.getDentistId()).getName();
        return ComplaintResponse.of(complaint, patientName, dentistName);
    }

    private void recordRead(ClinicPrincipal caller, String target) {
        record(caller, "COMPLAINT_READ", target);
    }

    private void record(ClinicPrincipal caller, String action, String targetId) {
        audit.save(AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .actorUid(caller.uid())
                .actorRole(caller.role().name())
                .action(action)
                .targetType("Complaint")
                .targetId(targetId)
                .timestamp(Instant.now())
                .build());
    }

    private static ComplaintCategory requireCategory(ComplaintCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Choose what your concern is about.");
        }
        return category;
    }

    private static String requireDetail(String detail) {
        String trimmed = detail == null ? "" : detail.trim();
        if (trimmed.length() < MIN_DETAIL) {
            throw new IllegalArgumentException(
                    "Please describe what happened, in a sentence or two at least — the"
                            + " administrator can only act on what you tell them.");
        }
        if (trimmed.length() > MAX_DETAIL) {
            throw new IllegalArgumentException("That is longer than " + MAX_DETAIL + " characters.");
        }
        return trimmed;
    }
}
