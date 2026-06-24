package com.flowforge.model.json;

import com.flowforge.exception.JsonException;

import java.util.List;
import java.util.Map;

/**
 * Navigates a parsed JSON object tree (see {@link JsonParser}) using a
 * simple dot/bracket path, returning the value at that path as a string.
 * <p>
 * Path syntax (similar to n8n's expression dot-notation):
 * <pre>
 *   user.name            -&gt; object key access
 *   items.0.title        -&gt; array index access
 *   items[1].price       -&gt; bracket index access (equivalent to items.1.price)
 * </pre>
 * The empty path returns the whole document stringified.
 */
public final class JsonPath {

    private JsonPath() {
    }

    /**
     * Resolves {@code path} against the parsed {@code root}.
     *
     * @return the value at the path, rendered as a string
     * @throws JsonException if any segment does not exist
     */
    public static String read(Object root, String path) throws JsonException {
        if (path == null || path.isBlank()) {
            return stringify(root);
        }
        Object current = root;
        StringBuilder traversed = new StringBuilder();
        for (String segment : split(path)) {
            current = step(current, segment, traversed.toString());
            traversed.append(traversed.isEmpty() ? "" : ".").append(segment);
        }
        return stringify(current);
    }

    private static Object step(Object current, String segment, String traversed)
            throws JsonException {
        String where = traversed.isEmpty() ? "<root>" : traversed;
        if (current == null) {
            throw new JsonException("Cannot read '" + segment + "': value at "
                    + where + " is null.");
        }
        if (current instanceof Map<?, ?> map) {
            if (!map.containsKey(segment)) {
                throw new JsonException("Key '" + segment + "' not found under " + where + ".");
            }
            return map.get(segment);
        }
        if (current instanceof List<?> list) {
            int index = parseIndex(segment, where);
            if (index < 0 || index >= list.size()) {
                throw new JsonException("Index " + index + " out of bounds under "
                        + where + " (size " + list.size() + ").");
            }
            return list.get(index);
        }
        throw new JsonException("Cannot descend into '" + segment + "' under "
                + where + ": value is a scalar.");
    }

    private static int parseIndex(String segment, String where) throws JsonException {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            throw new JsonException("Expected an array index under " + where
                    + " but found '" + segment + "'.");
        }
    }

    /** Splits a path into segments, treating {@code [n]} the same as {@code .n}. */
    private static String[] split(String path) {
        return path.replace("[", ".").replace("]", "").split("\\.");
    }

    /** Renders a resolved JSON value as a string for storage in a variable. */
    public static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) (double) d);
            }
            return String.valueOf((double) d);
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return JsonWriter.write(value);
        }
        return value.toString();
    }
}
