package com.flowforge.model.json;

import com.flowforge.exception.JsonException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the hand-written {@link JsonParser}: the full grammar, nesting,
 * escapes and error reporting.
 */
class JsonParserTest {

    @Test
    void parsesFlatObject() throws Exception {
        Object root = JsonParser.parse("{\"name\":\"Mahir\",\"age\":21}");
        assertInstanceOf(Map.class, root);
        Map<?, ?> map = (Map<?, ?>) root;
        assertEquals("Mahir", map.get("name"));
        assertEquals(21.0, map.get("age"));
    }

    @Test
    void parsesNestedStructures() throws Exception {
        Object root = JsonParser.parse(
                "{\"items\":[{\"id\":1},{\"id\":2}],\"ok\":true,\"none\":null}");
        Map<?, ?> map = (Map<?, ?>) root;
        assertEquals(Boolean.TRUE, map.get("ok"));
        assertNull(map.get("none"));
        List<?> items = (List<?>) map.get("items");
        assertEquals(2, items.size());
        assertEquals(1.0, ((Map<?, ?>) items.get(0)).get("id"));
    }

    @Test
    void parsesArraysAtRoot() throws Exception {
        Object root = JsonParser.parse("[1, 2, 3]");
        assertInstanceOf(List.class, root);
        assertEquals(3, ((List<?>) root).size());
    }

    @Test
    void handlesEscapesAndUnicode() throws Exception {
        Object root = JsonParser.parse("{\"text\":\"line1\\nline2 \\u0041\"}");
        assertEquals("line1\nline2 A", ((Map<?, ?>) root).get("text"));
    }

    @Test
    void parsesNegativeAndDecimalNumbers() throws Exception {
        Object root = JsonParser.parse("{\"a\":-3.5,\"b\":2e2}");
        Map<?, ?> map = (Map<?, ?>) root;
        assertEquals(-3.5, map.get("a"));
        assertEquals(200.0, map.get("b"));
    }

    @Test
    void emptyContainersParse() throws Exception {
        assertTrue(((Map<?, ?>) JsonParser.parse("{}")).isEmpty());
        assertTrue(((List<?>) JsonParser.parse("[]")).isEmpty());
    }

    @Test
    void rejectsNullInput() {
        assertThrows(JsonException.class, () -> JsonParser.parse(null));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{} extra"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{\"x\":\"oops}"));
    }

    @Test
    void rejectsMissingComma() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{\"a\":1 \"b\":2}"));
    }

    @Test
    void booleanFalseIsRecognised() throws Exception {
        assertFalse((Boolean) ((Map<?, ?>) JsonParser.parse("{\"x\":false}")).get("x"));
    }
}
