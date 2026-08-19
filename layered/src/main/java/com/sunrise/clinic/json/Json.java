package com.sunrise.clinic.json;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader/writer written for this project.
 *
 * <p>The application uses no web framework, so there is no library doing the
 * object&harr;JSON conversion for us. This class provides just the two operations
 * the API needs:</p>
 *
 * <ul>
 *   <li>{@link #write(Object)} — serialise maps, lists, records, enums, numbers,
 *       booleans, {@code null} and the {@code java.time} types the domain uses.
 *       Records are walked reflectively via {@link Class#getRecordComponents()},
 *       so every DTO serialises without a hand-written mapper.</li>
 *   <li>{@link #parseObject(String)} — parse a JSON object into a
 *       {@code Map<String, Object>}, which is all the request bodies require.</li>
 * </ul>
 *
 * <p>Dates are written in ISO-8601 ({@code 2026-07-20}, {@code 16:30}), matching
 * what the previous API emitted so the front-end contract is unchanged.</p>
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    /** Serialise a value to a JSON string. */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    private static void writeValue(Object value, StringBuilder out) {
        // Written as an if/else chain rather than a pattern-matching switch so the
        // project compiles on Java 17, the version the build targets.
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            writeString(s, out);
        } else if (value instanceof Boolean b) {
            out.append(b);
        } else if (value instanceof Double d) {
            writeDouble(d, out);
        } else if (value instanceof Float f) {
            writeDouble(f.doubleValue(), out);
        } else if (value instanceof Number n) {
            out.append(n);
        } else if (value instanceof Enum<?> e) {
            writeString(e.name(), out);
        } else if (value instanceof LocalDate d) {
            writeString(d.toString(), out);
        } else if (value instanceof LocalTime t) {
            writeString(t.toString(), out);
        } else if (value instanceof Instant i) {
            writeString(i.toString(), out);
        } else if (value instanceof Map<?, ?> m) {
            writeMap(m, out);
        } else if (value instanceof Iterable<?> it) {
            writeArray(it, out);
        } else if (value instanceof Object[] arr) {
            writeArray(List.of(arr), out);
        } else if (value.getClass().isRecord()) {
            writeRecord(value, out);
        } else {
            writeString(String.valueOf(value), out);
        }
    }

    /** Whole doubles are written without a trailing ".0" so money reads naturally. */
    private static void writeDouble(double d, StringBuilder out) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            out.append("null");
        } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            out.append((long) d);
        } else {
            out.append(d);
        }
    }

    private static void writeMap(Map<?, ?> map, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(String.valueOf(e.getKey()), out);
            out.append(':');
            writeValue(e.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(Iterable<?> items, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeValue(item, out);
        }
        out.append(']');
    }

    private static void writeRecord(Object record, StringBuilder out) {
        out.append('{');
        RecordComponent[] components = record.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            writeString(components[i].getName(), out);
            out.append(':');
            try {
                writeValue(components[i].getAccessor().invoke(record), out);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Cannot read record component " + components[i].getName(), e);
            }
        }
        out.append('}');
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Parse a JSON object.
     *
     * @param json the request body
     * @return the parsed fields; an empty map when the body is blank
     * @throws IllegalArgumentException if the text is not a well-formed JSON object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Trailing content after JSON value");
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    /** Read a String field, or null when absent. */
    public static String string(Map<String, Object> obj, String field) {
        Object v = obj.get(field);
        return v == null ? null : String.valueOf(v);
    }

    /** Read an int field, falling back to {@code defaultValue} when absent. */
    public static int integer(Map<String, Object> obj, String field, int defaultValue) {
        Object v = obj.get(field);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v).trim());
    }

    /** A minimal recursive-descent JSON parser. */
    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        boolean atEnd() {
            return pos >= src.length();
        }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObjectValue();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObjectValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
                }
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new IllegalArgumentException("Unterminated string");
                }
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char esc = next();
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw new IllegalArgumentException("Truncated \\u escape");
                        }
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new IllegalArgumentException("Bad escape \\" + esc);
                }
            }
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < src.length() && "+-.eE0123456789".indexOf(src.charAt(pos)) >= 0) {
                pos++;
            }
            String text = src.substring(start, pos);
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Expected a value at position " + start);
            }
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                return Double.valueOf(text);
            }
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException e) {
                return Long.valueOf(text);
            }
        }

        private char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return src.charAt(pos);
        }

        private char next() {
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return src.charAt(pos++);
        }

        private void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw new IllegalArgumentException(
                        "Expected '" + expected + "' but found '" + c + "' at position " + (pos - 1));
            }
        }
    }
}
