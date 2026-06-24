package com.flowforge.model.json;

import com.flowforge.exception.JsonException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser.
 * <p>
 * It supports the full JSON grammar (objects, arrays, strings, numbers,
 * booleans and null) and produces a plain Java object tree:
 * <ul>
 *   <li>object &rarr; {@code Map<String, Object>} (insertion-ordered)</li>
 *   <li>array  &rarr; {@code List<Object>}</li>
 *   <li>string &rarr; {@code String}</li>
 *   <li>number &rarr; {@code Double}</li>
 *   <li>boolean&rarr; {@code Boolean}</li>
 *   <li>null   &rarr; {@code null}</li>
 * </ul>
 * A hand-written recursive-descent parser is used rather than a third-party
 * library so the project has no runtime dependency for JSON handling, and so
 * the parsing technique itself is on display.
 */
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    /** Parses a JSON document into a Java object tree. */
    public static Object parse(String json) throws JsonException {
        if (json == null) {
            throw new JsonException("Cannot parse null JSON.");
        }
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonException("Unexpected trailing characters at position " + parser.pos);
        }
        return value;
    }

    // ---------- grammar ----------

    private Object readValue() throws JsonException {
        if (atEnd()) {
            throw new JsonException("Unexpected end of JSON input.");
        }
        char c = peek();
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't', 'f' -> readBoolean();
            case 'n' -> readNull();
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() throws JsonException {
        Map<String, Object> object = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            object.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return object;
            }
            if (c != ',') {
                throw new JsonException("Expected ',' or '}' in object at position " + pos);
            }
        }
    }

    private List<Object> readArray() throws JsonException {
        List<Object> array = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return array;
        }
        while (true) {
            skipWhitespace();
            array.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return array;
            }
            if (c != ',') {
                throw new JsonException("Expected ',' or ']' in array at position " + pos);
            }
        }
    }

    private String readString() throws JsonException {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (atEnd()) {
                throw new JsonException("Unterminated string in JSON.");
            }
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                sb.append(readEscape());
            } else {
                sb.append(c);
            }
        }
    }

    private char readEscape() throws JsonException {
        char c = next();
        return switch (c) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicode();
            default -> throw new JsonException("Invalid escape '\\" + c + "' in JSON.");
        };
    }

    private char readUnicode() throws JsonException {
        if (pos + 4 > text.length()) {
            throw new JsonException("Incomplete unicode escape in JSON.");
        }
        String hex = text.substring(pos, pos + 4);
        pos += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new JsonException("Invalid unicode escape '\\u" + hex + "' in JSON.");
        }
    }

    private Boolean readBoolean() throws JsonException {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonException("Invalid literal at position " + pos);
    }

    private Object readNull() throws JsonException {
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonException("Invalid literal at position " + pos);
    }

    private Double readNumber() throws JsonException {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (!atEnd() && isNumberChar(peek())) {
            pos++;
        }
        String number = text.substring(start, pos);
        if (number.isEmpty() || number.equals("-")) {
            throw new JsonException("Invalid number at position " + start);
        }
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            throw new JsonException("Invalid number '" + number + "' in JSON.");
        }
    }

    private static boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
    }

    // ---------- low-level helpers ----------

    private void skipWhitespace() {
        while (!atEnd() && Character.isWhitespace(peek())) {
            pos++;
        }
    }

    private char peek() {
        return text.charAt(pos);
    }

    private char next() throws JsonException {
        if (atEnd()) {
            throw new JsonException("Unexpected end of JSON input.");
        }
        return text.charAt(pos++);
    }

    private void expect(char expected) throws JsonException {
        char c = next();
        if (c != expected) {
            throw new JsonException("Expected '" + expected + "' but found '" + c
                    + "' at position " + (pos - 1));
        }
    }

    private boolean atEnd() {
        return pos >= text.length();
    }
}
