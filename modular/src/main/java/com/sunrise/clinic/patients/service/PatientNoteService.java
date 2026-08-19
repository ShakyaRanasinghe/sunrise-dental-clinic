package com.sunrise.clinic.patients.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.patients.data.PatientNoteRepository;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.patients.domain.NoteCategory;
import com.sunrise.clinic.patients.domain.PatientNote;
import com.sunrise.clinic.patients.domain.PatientNoteResponse;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The patient's own medical notes - FR-NOTE-01 to FR-NOTE-12.
 *
 * <h2>Three rules pointing three different ways</h2>
 *
 * <ul>
 *   <li>the <b>patient</b> writes, edits and deletes their own notes, and nobody else's
 *       (FR-NOTE-01, 05);</li>
 *   <li>a <b>dentist</b> reads the notes of patients on their own schedule, and cannot write
 *       any (FR-NOTE-09, 10). The patient owns their own record: a dentist who disagrees with
 *       a declared fact records that as a diagnosis, not by editing what the patient said;</li>
 *   <li>a <b>receptionist</b> and an <b>administrator</b> see nothing at all (FR-NOTE-11).
 *       Not a redacted note - nothing. There is no response shape that would carry one to
 *       them, so no screen can leak one by accident.</li>
 * </ul>
 *
 * <p>Enforced here rather than at each screen, because the same three rules apply to the
 * profile page, the dentist's schedule, and four API routes.</p>
 */
public class PatientNoteService {

    private static final Logger log = Logger.getLogger(PatientNoteService.class.getName());

    /** Long enough for a real medical fact, short enough not to become a diary. */
    private static final int MIN_DETAIL = 3;
    private static final int MAX_DETAIL = 1000;

    private final PatientNoteRepository notes;
    private final PatientRepository patients;
    private final TreatmentRelationship treating;

    public PatientNoteService(PatientNoteRepository notes, PatientRepository patients,
                             TreatmentRelationship treating) {
        this.notes = notes;
        this.patients = patients;
        this.treating = treating;
    }

    // --- reading ------------------------------------------------------

    /**
     * The notes for a patient.
     *
     * @return the notes, critical first. An <b>empty list is a real answer</b> - the caller
     *         must render "nothing declared" rather than a blank space, because a blank looks
     *         like a screen that failed to load and a dentist cannot tell the difference
     *         (FR-NOTE-12)
     */
    public List<PatientNoteResponse> forPatient(ClinicPrincipal caller, String patientId) {
        requireMayRead(caller, patientId);
        return notes.findByPatientId(patientId).stream().map(PatientNoteResponse::of).toList();
    }

    /** The signed-in patient's own notes. */
    public List<PatientNoteResponse> own(ClinicPrincipal caller) {
        return forPatient(caller, requireOwnPatientId(caller));
    }

    /**
     * Whether this patient has declared anything critical - FR-NOTE-08.
     *
     * <p>Used by the dentist's schedule to show a banner before the appointment is opened.
     * Answering only true or false means the schedule can warn without loading the notes for
     * every patient on the day.</p>
     */
    public boolean hasCriticalNotes(ClinicPrincipal caller, String patientId) {
        requireMayRead(caller, patientId);
        return notes.hasCritical(patientId);
    }

    // --- writing, patient only ----------------------------------------

    /** Declare a note - FR-NOTE-01. */
    public PatientNoteResponse declare(ClinicPrincipal caller, NoteCategory category,
                                       String detail, boolean critical) {
        String patientId = requireOwnPatientId(caller);
        AccessControl.require(caller, Action.DECLARE_OWN_NOTES);

        PatientNote note = PatientNote.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .category(requireCategory(category))
                .detail(requireDetail(detail))
                .critical(critical)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        notes.save(note);

        // The detail is medical information about a named person and is never logged.
        log.log(Level.INFO, "note_declared patient={0} category={1} critical={2}",
                new Object[] { patientId, note.getCategory(), critical });
        return PatientNoteResponse.of(note);
    }

    /**
     * Correct a note - FR-NOTE-05.
     *
     * <p>{@code createdAt} is untouched: a corrected note is still one the patient declared
     * on the day they first declared it.</p>
     */
    public PatientNoteResponse amend(ClinicPrincipal caller, String noteId,
                                     NoteCategory category, String detail, boolean critical) {
        PatientNote note = requireOwnNote(caller, noteId);
        note.setCategory(requireCategory(category));
        note.setDetail(requireDetail(detail));
        note.setCritical(critical);
        note.setUpdatedAt(Instant.now());
        notes.save(note);
        return PatientNoteResponse.of(note);
    }

    /**
     * Withdraw a note - FR-NOTE-05.
     *
     * <p>Deleted rather than marked withdrawn. A medical fact that is no longer true and
     * cannot be removed is worse than no record: the dentist reads a warning that has ceased
     * to apply and treats accordingly.</p>
     */
    public void withdraw(ClinicPrincipal caller, String noteId) {
        PatientNote note = requireOwnNote(caller, noteId);
        notes.deleteById(note.getId());
        log.log(Level.INFO, "note_withdrawn patient={0} category={1}",
                new Object[] { note.getPatientId(), note.getCategory() });
    }

    // --- the rules ----------------------------------------------------

    /**
     * @throws AccessControl.AccessDeniedException unless the caller is this patient, or a
     *         dentist treating them
     */
    private void requireMayRead(ClinicPrincipal caller, String patientId) {
        if (caller == null) {
            throw new AccessControl.NotAuthenticatedException("Authentication is required.");
        }
        switch (caller.role()) {
            case PATIENT -> {
                if (!patientId.equals(ownPatientId(caller))) {
                    throw new AccessControl.AccessDeniedException(
                            "Those notes are not yours.");
                }
            }
            case DENTIST -> {
                AccessControl.require(caller, Action.READ_CLINICAL);
                // FR-NOTE-09. Holding READ_CLINICAL is not enough: it makes a dentist
                // clinical staff, not this patient's dentist.
                if (!treating.isTreating(caller.uid(), patientId)) {
                    throw new AccessControl.AccessDeniedException(
                            "You can only read notes for patients on your own schedule.");
                }
            }
            // FR-NOTE-11. Not a redacted view — nothing.
            case RECEPTIONIST, ADMIN -> throw new AccessControl.AccessDeniedException(
                    "Medical notes are between the patient and their dentist.");
        }
    }

    private PatientNote requireOwnNote(ClinicPrincipal caller, String noteId) {
        String patientId = requireOwnPatientId(caller);
        AccessControl.require(caller, Action.DECLARE_OWN_NOTES);
        PatientNote note = notes.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("No note " + noteId));
        if (!note.belongsTo(patientId)) {
            // Not found rather than forbidden: confirming the note exists would tell a
            // caller that some other patient has declared something.
            throw new ResourceNotFoundException("No note " + noteId);
        }
        return note;
    }

    private String requireOwnPatientId(ClinicPrincipal caller) {
        if (caller == null || caller.role() != Role.PATIENT) {
            throw new AccessControl.AccessDeniedException(
                    "Only a patient can declare notes about themselves.");
        }
        String patientId = ownPatientId(caller);
        if (patientId == null) {
            throw new ResourceNotFoundException(
                    "Your account has no patient record. Ask the clinic to add one.");
        }
        return patientId;
    }

    private String ownPatientId(ClinicPrincipal caller) {
        return patients.findByUserUid(caller.uid()).map(Patient::getId).orElse(null);
    }

    private static NoteCategory requireCategory(NoteCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Choose what kind of note this is.");
        }
        return category;
    }

    private static String requireDetail(String detail) {
        String trimmed = detail == null ? "" : detail.trim();
        if (trimmed.length() < MIN_DETAIL) {
            throw new IllegalArgumentException(
                    "Say a little more, so the dentist knows what to look out for.");
        }
        if (trimmed.length() > MAX_DETAIL) {
            throw new IllegalArgumentException(
                    "That is longer than " + MAX_DETAIL + " characters. Keep it to the facts a"
                            + " dentist needs before treating.");
        }
        return trimmed;
    }
}
