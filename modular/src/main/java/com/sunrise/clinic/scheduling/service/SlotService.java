package com.sunrise.clinic.scheduling.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.scheduling.data.SessionRepository;
import com.sunrise.clinic.scheduling.data.SlotRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.DentistSession;
import com.sunrise.clinic.scheduling.domain.Slot;
import com.sunrise.clinic.scheduling.domain.SlotResponse;
import com.sunrise.clinic.scheduling.domain.SlotStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Availability. A receptionist publishes a {@link DentistSession} - a window like
 * "Dr. Silva, Tuesday, 16:00-18:00" - and this service explodes it into fixed-length
 * bookable {@link Slot}s. Patients then browse the open ones.
 *
 * <p>Session and slot are a composition, not an association: a slot has no meaning
 * apart from the window it came from, which is why {@code slot.session_id} cascades
 * on delete. Deleting a published window removes the slots it produced.</p>
 *
 * <h2>What this version validates that the previous one did not</h2>
 *
 * <p>{@code publishSession} accepted almost anything. Four checks were missing, and
 * each produced a wrong outcome rather than an error message:</p>
 *
 * <ol>
 *   <li><b>An unknown {@code dentistId}</b> reached the insert, violated
 *       {@code fk_session_dentist} and surfaced as a 500 with a SQL message. Now a
 *       404 that names the dentist.</li>
 *   <li><b>An overlapping window</b> for the same dentist was accepted, producing two
 *       sessions covering the same hour. Because a slot id is
 *       {@code dentistId_date_startTime}, the second window's slots silently
 *       overwrote the first's - and any slot already booked in the overlap was
 *       reset to OPEN, so a patient's appointment quietly lost its slot.</li>
 *   <li><b>A date in the past</b> was accepted, publishing availability nobody can
 *       book.</li>
 *   <li><b>A window that does not divide evenly</b> lost the remainder without
 *       saying so: 16:00-17:20 in 30-minute slots produced two slots and discarded
 *       twenty minutes. Now the caller is told.</li>
 * </ol>
 */
public class SlotService {

    private static final Logger log = Logger.getLogger(SlotService.class.getName());

    private final SessionRepository sessions;
    private final SlotRepository slots;
    private final ReferenceService reference;

    public SlotService(SessionRepository sessions, SlotRepository slots, ReferenceService reference) {
        this.sessions = sessions;
        this.slots = slots;
        this.reference = reference;
    }

    /**
     * Publish an availability window and generate its bookable slots.
     *
     * <p>A slot's id is deterministic - {@code dentistId_date_startTime} - so
     * republishing the identical window is idempotent rather than duplicating slots.
     * That property is also why overlapping windows had to be refused: determinism
     * turns an overlap into a silent overwrite instead of a duplicate row.</p>
     *
     * @return the session, the slots it produced, and any warning the caller should
     *         show
     */
    public Published publishSession(ClinicPrincipal caller, NewSession request) {
        AccessControl.require(caller, Action.PUBLISH_AVAILABILITY);

        // Fix 1: a dentist who does not exist is a 404, not a foreign-key violation.
        Dentist dentist = reference.requireDentist(request.dentistId());

        LocalDate date = request.date();
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        int slotMinutes = request.slotMinutes();

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (slotMinutes <= 0) {
            throw new IllegalArgumentException("slotMinutes must be greater than zero");
        }
        // Fix 3: availability nobody can book is not availability.
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("date cannot be in the past");
        }
        long windowMinutes = Duration.between(start, end).toMinutes();
        if (slotMinutes > windowMinutes) {
            throw new IllegalArgumentException(
                    "slotMinutes (" + slotMinutes + ") is longer than the window ("
                            + windowMinutes + " minutes), so no slot would fit");
        }

        // Fix 2: an overlap would overwrite the earlier window's slots, including any
        // already booked - so it is refused before anything is written.
        DentistSession clash = overlapping(request.dentistId(), date, start, end);
        if (clash != null) {
            // No "Dr." prefix: dentist.name already carries the title as stored
            // ("Dr. Ranil Silva"), and prepending produced "Dr. Dr. Ranil Silva".
            throw new IllegalArgumentException(
                    dentist.getName() + " already has " + clash.getStartTime()
                            + "-" + clash.getEndTime() + " published on " + date
                            + ". Withdraw that window before publishing one that overlaps it.");
        }

        DentistSession session = DentistSession.builder()
                .id(UUID.randomUUID().toString())
                .dentistId(request.dentistId())
                .date(date)
                .startTime(start)
                .endTime(end)
                .slotDurationMinutes(slotMinutes)
                .publishedByUid(caller.uid())
                .build();
        sessions.save(session);

        List<Slot> created = new ArrayList<>();
        LocalTime cursor = start;
        while (!cursor.plusMinutes(slotMinutes).isAfter(end)) {
            Slot slot = Slot.builder()
                    .id(slotId(request.dentistId(), date, cursor))
                    .sessionId(session.getId())
                    .dentistId(request.dentistId())
                    .date(date)
                    .startTime(cursor)
                    .durationMinutes(slotMinutes)
                    .status(SlotStatus.OPEN)
                    .build();
            slots.save(slot);
            created.add(slot);
            cursor = cursor.plusMinutes(slotMinutes);
        }

        // Fix 4: say so rather than discarding the remainder in silence.
        String warning = null;
        long remainder = windowMinutes % slotMinutes;
        if (remainder > 0) {
            warning = "The window is " + windowMinutes + " minutes, which does not divide evenly"
                    + " into " + slotMinutes + "-minute slots. The last " + remainder
                    + " minutes are not bookable.";
        }

        log.log(Level.INFO, "availability_published dentist={0} date={1} slots={2} by={3}",
                new Object[] { request.dentistId(), date, created.size(), caller.uid() });

        return new Published(session, describe(created, dentist.getName()), warning);
    }

    /** Open slots for one dentist on one date - what a patient sees for "today". */
    public List<SlotResponse> openSlots(String dentistId, LocalDate date) {
        Dentist dentist = reference.requireDentist(dentistId);
        return slots.findByDentistIdAndDate(dentistId, date).stream()
                .filter(slot -> slot.getStatus() == SlotStatus.OPEN)
                .sorted(Comparator.comparing(Slot::getStartTime))
                .map(slot -> SlotResponse.of(slot, dentist.getName()))
                .toList();
    }

    /**
     * Every slot for one dentist on one date, booked ones included.
     *
     * <p>Distinct from {@link #openSlots} on purpose. A patient browsing should see
     * only what they can book; the receptionist publishing needs to see what is
     * already taken, because a window they are about to replace may have bookings in
     * it.</p>
     */
    public List<SlotResponse> allSlots(String dentistId, LocalDate date) {
        Dentist dentist = reference.requireDentist(dentistId);
        return slots.findByDentistIdAndDate(dentistId, date).stream()
                .sorted(Comparator.comparing(Slot::getStartTime))
                .map(slot -> SlotResponse.of(slot, dentist.getName()))
                .toList();
    }

    /**
     * Open slots across a date range - what a patient sees for "this week".
     *
     * <p>Resolves every dentist named in the results in one pass rather than once per
     * slot: a week of two dentists at 30-minute slots is around sixty rows, and a
     * lookup each would be sixty queries to print two names.</p>
     */
    public List<SlotResponse> openSlotsBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        List<Slot> open = slots.findByDateBetween(from, to).stream()
                .filter(slot -> slot.getStatus() == SlotStatus.OPEN)
                .sorted(Comparator.comparing(Slot::getDate).thenComparing(Slot::getStartTime))
                .toList();
        Map<String, String> names = namesFor(open);
        return open.stream()
                .map(slot -> SlotResponse.of(slot, names.get(slot.getDentistId())))
                .toList();
    }

    /** Every window published for a dentist, most recent first. */
    public List<DentistSession> publishedFor(String dentistId) {
        return sessions.findByDentistId(dentistId).stream()
                .sorted(Comparator.comparing(DentistSession::getDate).reversed()
                        .thenComparing(DentistSession::getStartTime))
                .toList();
    }

    /** What the caller supplies to publish a window. */
    public record NewSession(String dentistId,
                             LocalDate date,
                             LocalTime startTime,
                             LocalTime endTime,
                             int slotMinutes) {
    }

    /**
     * The outcome of publishing.
     *
     * @param warning non-null when the window does not divide evenly into slots. A
     *                warning rather than a refusal: 16:00-17:20 in 30-minute slots is
     *                a legitimate thing to publish, and the caller should simply know
     *                that twenty minutes are not bookable.
     */
    public record Published(DentistSession session, List<SlotResponse> slots, String warning) {

        public boolean hasWarning() {
            return warning != null;
        }
    }

    /**
     * @return an already-published window for this dentist and date that overlaps
     *         {@code [start, end)}, or null
     */
    private DentistSession overlapping(String dentistId, LocalDate date,
                                       LocalTime start, LocalTime end) {
        return sessions.findByDentistId(dentistId).stream()
                .filter(existing -> date.equals(existing.getDate()))
                // Half-open intervals, so 16:00-17:00 and 17:00-18:00 are adjacent
                // rather than overlapping - which is how a full day gets published.
                .filter(existing -> start.isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(end))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> namesFor(List<Slot> forSlots) {
        return forSlots.stream()
                .map(Slot::getDentistId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(),
                        id -> reference.requireDentist(id).getName()));
    }

    private static List<SlotResponse> describe(List<Slot> created, String dentistName) {
        return created.stream().map(slot -> SlotResponse.of(slot, dentistName)).toList();
    }

    /**
     * Deterministic, so republishing the same window is idempotent.
     *
     * <p>{@code LocalTime.toString()} drops the seconds when they are zero, giving
     * {@code d-silva_2026-08-20_16:00}. Stable for a given input, which is all the id
     * needs to be.</p>
     */
    private static String slotId(String dentistId, LocalDate date, LocalTime start) {
        return dentistId + "_" + date + "_" + start;
    }
}
