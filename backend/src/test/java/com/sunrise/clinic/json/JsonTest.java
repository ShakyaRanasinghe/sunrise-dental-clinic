package com.sunrise.clinic.json;

import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.dto.SlotResponse;
import com.sunrise.clinic.domain.SlotStatus;
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
class JsonTest {

    // ---------------- writing ----------------

    @Test
    void writesRecordsFieldByField() {
        SlotResponse slot = new SlotResponse("s1", "d1",
                LocalDate.of(2026, 7, 20), LocalTime.of(16, 30), 30, SlotStatus.OPEN);

        assertEquals("{\"id\":\"s1\",\"dentistId\":\"d1\",\"date\":\"2026-07-20\","
                        + "\"startTime\":\"16:30\",\"durationMinutes\":30,\"status\":\"OPEN\"}",
                Json.write(slot));
    }

    @Test
    void writesEnumsAsTheirName() {
        assertEquals("\"CONFIRMED\"", Json.write(AppointmentStatus.CONFIRMED));
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
}
