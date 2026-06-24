package com.flowforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ExecutionContext}: variable storage and the
 * {@code ${name}} interpolation used by every task.
 */
class ExecutionContextTest {

    @Test
    void storesAndRetrievesValues() {
        ExecutionContext context = new ExecutionContext();
        context.put("user", "Mahir");
        assertEquals("Mahir", context.get("user"));
        assertTrue(context.contains("user"));
    }

    @Test
    void nullValueStoredAsEmptyString() {
        ExecutionContext context = new ExecutionContext();
        context.put("x", null);
        assertEquals("", context.get("x"));
    }

    @Test
    void missingKeyReturnsNull() {
        assertNull(new ExecutionContext().get("missing"));
    }

    @Test
    void interpolateReplacesKnownPlaceholders() {
        ExecutionContext context = new ExecutionContext();
        context.put("a", "1");
        context.put("b", "2");
        assertEquals("1 + 2", context.interpolate("${a} + ${b}"));
    }

    @Test
    void interpolateLeavesUnknownPlaceholders() {
        ExecutionContext context = new ExecutionContext();
        assertEquals("${missing}", context.interpolate("${missing}"));
    }

    @Test
    void interpolateHandlesNullAndEmpty() {
        ExecutionContext context = new ExecutionContext();
        assertNull(context.interpolate(null));
        assertEquals("", context.interpolate(""));
    }

    @Test
    void clearRemovesEverything() {
        ExecutionContext context = new ExecutionContext();
        context.put("x", "1");
        context.clear();
        assertFalse(context.contains("x"));
    }

    @Test
    void snapshotIsIndependentCopy() {
        ExecutionContext context = new ExecutionContext();
        context.put("x", "1");
        var snapshot = context.snapshot();
        context.put("y", "2");
        assertFalse(snapshot.containsKey("y"));
    }
}
