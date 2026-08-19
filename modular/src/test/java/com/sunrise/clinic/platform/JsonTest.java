package com.sunrise.clinic.platform;

import com.sunrise.clinic.platform.json.Json;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-J01..J09 — the hand-written JSON reader/writer.
 *
 * <p>This class replaced a third-party serialisation library, so it needs tests
 * that the library previously made unnecessary: the escaping rules, the date
 * formats the front-end depends on, and rejection of malformed input.</p>
 */
public class JsonTest {

    // ---------------- writing ----------------

    @Test
    void writesRecordsFieldByField() {
        Slot slot = new Slot("s1", "d1",
                LocalDate.of(2026, 7, 20), LocalTime.of(16, 30), 30, Status.OPEN);

        assertEquals("{\"id\":\"s1\",\"dentistId\":\"d1\",\"date\":\"2026-07-20\","
                        + "\"startTime\":\"16:30\",\"durationMinutes\":30,\"status\":\"OPEN\"}",
                Json.write(slot));
    }

    @Test
    void writesEnumsAsTheirName() {
        assertEquals("\"CONFIRMED\"", Json.write(Status.CONFIRMED));
    }

    @Test
    void writesNullAsJsonNull() {
        assertEquals("null", Json.write(null));
        assertEquals("{\"a\":null}", Json.write(java.util.Collections.singletonMap("a", null)));
    }

    @Test
    void writesWholeAmountsWithoutATrailingZero() {
        // Money reads badly as "1500.0" on a receipt.
        assertEquals("1500", Json.write(1500.0));
        assertEquals("1500.75", Json.write(1500.75));
    }

    @Test
    void escapesCharactersThatWouldBreakTheDocument() {
        String written = Json.write(Map.of("note", "He said \"ouch\"\nthen \\ left"));
        assertTrue(written.contains("\\\"ouch\\\""), written);
        assertTrue(written.contains("\\n"), written);
        assertTrue(written.contains("\\\\"), written);
    }

    @Test
    void writesNestedListsAndMaps() {
        assertEquals("{\"slots\":[1,2,3]}",
                Json.write(Map.of("slots", List.of(1, 2, 3))));
    }

    // ---------------- reading ----------------

    @Test
    void parsesAnObject() {
        Map<String, Object> parsed = Json.parseObject(
                "{\"slotId\":\"s1\",\"minutes\":30,\"ok\":true,\"missing\":null}");

        assertEquals("s1", Json.string(parsed, "slotId"));
        assertEquals(30, Json.integer(parsed, "minutes", 0));
        assertEquals(Boolean.TRUE, parsed.get("ok"));
        assertNull(parsed.get("missing"));
    }

    @Test
    void parsesEscapesBackToTheOriginalText() {
        Map<String, Object> parsed = Json.parseObject("{\"note\":\"line\\none\\ttab \\u0041\"}");
        assertEquals("line\none\ttab A", parsed.get("note"));
    }

    @Test
    void treatsAnEmptyBodyAsNoFields() {
        assertTrue(Json.parseObject("").isEmpty());
        assertTrue(Json.parseObject(null).isEmpty());
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"a\":}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"a\":1"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1,2]"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"a\":1} trailing"));
    }

    @Test
    void integerFallsBackWhenTheFieldIsAbsent() {
        assertEquals(30, Json.integer(Json.parseObject("{}"), "slotMinutes", 30));
    }

    @Test
    void roundTripsAValueThroughWriteAndParse() {
        String written = Json.write(Map.of("email", "nimal@example.lk", "attempts", 3));
        Map<String, Object> parsed = Json.parseObject(written);
        assertEquals("nimal@example.lk", parsed.get("email"));
        assertEquals(3, parsed.get("attempts"));
        assertFalse(parsed.containsKey("password"));
    }

    // ------------------------------------------------------------------
    //  The defect this branch exists for.
    //
    //  Before writeBean, anything that was not a record fell through to
    //  String.valueOf, so GET /api/dentists answered 200 with
    //  ["Dentist{id=d-silva}"] - a quoted toString carrying an id and nothing
    //  else. These tests fail against that behaviour.
    // ------------------------------------------------------------------

    /** Stands in for a domain class: private fields, getters, a terse toString. */
    public static final class Bean {
        private final String id = "d-silva";
        private final String name = "Dr. Ranil Silva";
        private final double consultationFee = 1500.0;
        private final boolean active = true;

        public String getId() { return id; }
        public String getName() { return name; }
        public double getConsultationFee() { return consultationFee; }
        public boolean isActive() { return active; }

        @Override public String toString() { return "Bean{id=" + id + "}"; }
    }

    @Test
    void writesAPlainObjectAsAnObjectAndNotAsItsToString() {
        String json = Json.write(new Bean());

        assertTrue(json.startsWith("{"), "should be a JSON object, was: " + json);
        assertFalse(json.contains("Bean{"), "fell through to toString: " + json);
        assertTrue(json.contains("\"name\":\"Dr. Ranil Silva\""), json);
        assertTrue(json.contains("\"consultationFee\":1500"), json);
        assertTrue(json.contains("\"active\":true"), json);
    }

    @Test
    void doesNotLeakGetClassAsAProperty() {
        assertFalse(Json.write(new Bean()).contains("class"), Json.write(new Bean()));
    }

    @Test
    void writesAListOfPlainObjectsAsAnArrayOfObjects() {
        String json = Json.write(java.util.List.of(new Bean(), new Bean()));
        assertTrue(json.startsWith("[{"), json);
        assertFalse(json.contains("Bean{"), json);
    }

    // ------------------------------------------------------------------
    //  Fixtures.
    //
    //  Deliberately local rather than the real SlotResponse and SlotStatus:
    //  platform/ depends on no feature module, and neither should its tests.
    //  The subject of these tests is the serialiser, so what it serialises is
    //  a fixture and the shape is all that matters.
    //
    //  They are public because reflection cannot read a package-private type
    //  from another package, and every real payload type is public too.
    // ------------------------------------------------------------------

    public enum Status { OPEN, BOOKED, CONFIRMED }

    public record Slot(String id, String dentistId, LocalDate date,
                LocalTime startTime, int durationMinutes, Status status) { }
}
