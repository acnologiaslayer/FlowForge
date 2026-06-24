package com.flowforge.model.json;

import com.flowforge.exception.JsonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JsonPath}: dot and bracket navigation, whole-tree
 * stringification and error reporting for bad paths.
 */
class JsonPathTest {

    private Object parse(String json) throws JsonException {
        return JsonParser.parse(json);
    }

    @Test
    void readsNestedObjectKey() throws Exception {
        Object root = parse("{\"user\":{\"name\":\"Mahir\"}}");
        assertEquals("Mahir", JsonPath.read(root, "user.name"));
    }

    @Test
    void readsArrayIndexWithDotNotation() throws Exception {
        Object root = parse("{\"items\":[\"a\",\"b\",\"c\"]}");
        assertEquals("b", JsonPath.read(root, "items.1"));
    }

    @Test
    void readsArrayIndexWithBracketNotation() throws Exception {
        Object root = parse("{\"items\":[{\"price\":9.5}]}");
        assertEquals("9.5", JsonPath.read(root, "items[0].price"));
    }

    @Test
    void integerValuedNumbersDropTrailingZero() throws Exception {
        Object root = parse("{\"count\":42}");
        assertEquals("42", JsonPath.read(root, "count"));
    }

    @Test
    void emptyPathStringifiesWholeTree() throws Exception {
        Object root = parse("{\"a\":1,\"b\":[2,3]}");
        String whole = JsonPath.read(root, "");
        assertTrue(whole.contains("\"a\":1"));
        assertTrue(whole.contains("\"b\":[2,3]"));
    }

    @Test
    void objectValueIsReserialised() throws Exception {
        Object root = parse("{\"user\":{\"id\":7}}");
        assertEquals("{\"id\":7}", JsonPath.read(root, "user"));
    }

    @Test
    void missingKeyThrows() throws Exception {
        Object root = parse("{\"a\":1}");
        assertThrows(JsonException.class, () -> JsonPath.read(root, "b"));
    }

    @Test
    void indexOutOfBoundsThrows() throws Exception {
        Object root = parse("{\"items\":[1]}");
        assertThrows(JsonException.class, () -> JsonPath.read(root, "items.5"));
    }

    @Test
    void descendingIntoScalarThrows() throws Exception {
        Object root = parse("{\"a\":1}");
        assertThrows(JsonException.class, () -> JsonPath.read(root, "a.b"));
    }

    @Test
    void nonNumericArrayIndexThrows() throws Exception {
        Object root = parse("{\"items\":[1,2]}");
        assertThrows(JsonException.class, () -> JsonPath.read(root, "items.first"));
    }
}
