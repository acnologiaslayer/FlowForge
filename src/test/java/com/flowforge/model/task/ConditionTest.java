package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link Condition} value object: numeric vs textual comparison,
 * interpolation, the unary empty checks and validation.
 */
class ConditionTest {

    private ExecutionContext context() {
        return new ExecutionContext();
    }

    private Condition condition(String left, Condition.Comparator op, String right)
            throws InvalidTaskConfigurationException {
        return new Condition(left, op, right);
    }

    @Test
    void numericEqualityComparesAsNumbers() throws Exception {
        assertTrue(condition("3.0", Condition.Comparator.EQUALS, "3").evaluate(context()));
    }

    @Test
    void textualEqualityWhenNotNumbers() throws Exception {
        assertTrue(condition("hello", Condition.Comparator.EQUALS, "hello").evaluate(context()));
        assertFalse(condition("hello", Condition.Comparator.EQUALS, "world").evaluate(context()));
    }

    @Test
    void greaterThanIsNumeric() throws Exception {
        assertTrue(condition("10", Condition.Comparator.GREATER, "9").evaluate(context()));
        // string comparison would make "10" < "9"; numeric must win
    }

    @Test
    void comparatorsCoverOrdering() throws Exception {
        assertTrue(condition("5", Condition.Comparator.GREATER_OR_EQUAL, "5").evaluate(context()));
        assertTrue(condition("4", Condition.Comparator.LESS, "5").evaluate(context()));
        assertTrue(condition("5", Condition.Comparator.LESS_OR_EQUAL, "5").evaluate(context()));
        assertTrue(condition("4", Condition.Comparator.NOT_EQUALS, "5").evaluate(context()));
    }

    @Test
    void containsAndNotContains() throws Exception {
        assertTrue(condition("flowforge", Condition.Comparator.CONTAINS, "forge")
                .evaluate(context()));
        assertTrue(condition("flowforge", Condition.Comparator.NOT_CONTAINS, "xyz")
                .evaluate(context()));
    }

    @Test
    void emptyChecksAreUnary() throws Exception {
        ExecutionContext ctx = context();
        ctx.put("blank", "");
        ctx.put("filled", "value");
        assertTrue(condition("${blank}", Condition.Comparator.IS_EMPTY, "").evaluate(ctx));
        assertTrue(condition("${filled}", Condition.Comparator.IS_NOT_EMPTY, "").evaluate(ctx));
        assertFalse(condition("${filled}", Condition.Comparator.IS_EMPTY, "").evaluate(ctx));
    }

    @Test
    void interpolatesBothOperands() throws Exception {
        ExecutionContext ctx = context();
        ctx.put("a", "7");
        ctx.put("b", "7");
        assertTrue(condition("${a}", Condition.Comparator.EQUALS, "${b}").evaluate(ctx));
    }

    @Test
    void unaryComparatorFlag() {
        assertTrue(Condition.Comparator.IS_EMPTY.isUnary());
        assertFalse(Condition.Comparator.EQUALS.isUnary());
    }

    @Test
    void comparatorRoundTripsByCode() {
        for (Condition.Comparator c : Condition.Comparator.values()) {
            assertEquals(c, Condition.Comparator.fromCode(c.getCode()));
        }
    }

    @Test
    void describeRendersReadableText() throws Exception {
        assertEquals("count is at least 3",
                condition("count", Condition.Comparator.GREATER_OR_EQUAL, "3").describe());
        assertEquals("name is empty",
                condition("name", Condition.Comparator.IS_EMPTY, "").describe());
    }

    @Test
    void blankLeftOperandRejected() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> condition("  ", Condition.Comparator.EQUALS, "1"));
    }

    @Test
    void nullComparatorRejected() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new Condition("a", null, "b"));
    }
}
