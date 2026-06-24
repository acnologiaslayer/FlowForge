package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

/**
 * A boolean test comparing a left value against a right value, used by the
 * branching ({@link IfTask}) and looping ({@link LoopTask}) steps.
 * <p>
 * This is a small <em>value object</em>: it is immutable and knows how to
 * evaluate itself against an {@link ExecutionContext}. Both operands support
 * {@code ${variable}} interpolation, and numeric operands are compared
 * numerically while everything else falls back to string comparison, which
 * mirrors the loose-but-predictable behaviour of n8n's IF node.
 */
public final class Condition {

    /** The comparison operators a condition may use. */
    public enum Comparator {
        EQUALS("==", "equals"),
        NOT_EQUALS("!=", "does not equal"),
        GREATER(">", "is greater than"),
        GREATER_OR_EQUAL(">=", "is at least"),
        LESS("<", "is less than"),
        LESS_OR_EQUAL("<=", "is at most"),
        CONTAINS("contains", "contains"),
        NOT_CONTAINS("!contains", "does not contain"),
        IS_EMPTY("empty", "is empty"),
        IS_NOT_EMPTY("!empty", "is not empty");

        private final String code;
        private final String label;

        Comparator(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        /** True for operators that ignore the right-hand operand. */
        public boolean isUnary() {
            return this == IS_EMPTY || this == IS_NOT_EMPTY;
        }

        public static Comparator fromCode(String code) {
            for (Comparator c : values()) {
                if (c.code.equalsIgnoreCase(code)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Unknown comparator: " + code);
        }
    }

    private final String left;
    private final Comparator comparator;
    private final String right;

    public Condition(String left, Comparator comparator, String right)
            throws InvalidTaskConfigurationException {
        if (comparator == null) {
            throw new InvalidTaskConfigurationException("Condition comparator must not be null.");
        }
        if (left == null || left.isBlank()) {
            throw new InvalidTaskConfigurationException("Condition left operand must not be blank.");
        }
        this.left = left.trim();
        this.comparator = comparator;
        this.right = right == null ? "" : right.trim();
    }

    public String getLeft() {
        return left;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public String getRight() {
        return right;
    }

    /** Evaluates the condition against the current variable state. */
    public boolean evaluate(ExecutionContext context) {
        String leftValue = context.interpolate(left);
        String rightValue = context.interpolate(right);

        return switch (comparator) {
            case IS_EMPTY -> leftValue == null || leftValue.isEmpty();
            case IS_NOT_EMPTY -> leftValue != null && !leftValue.isEmpty();
            case CONTAINS -> leftValue.contains(rightValue);
            case NOT_CONTAINS -> !leftValue.contains(rightValue);
            case EQUALS -> compareEquals(leftValue, rightValue);
            case NOT_EQUALS -> !compareEquals(leftValue, rightValue);
            case GREATER -> compareNumeric(leftValue, rightValue) > 0;
            case GREATER_OR_EQUAL -> compareNumeric(leftValue, rightValue) >= 0;
            case LESS -> compareNumeric(leftValue, rightValue) < 0;
            case LESS_OR_EQUAL -> compareNumeric(leftValue, rightValue) <= 0;
        };
    }

    /** Equality is numeric when both sides parse as numbers, else textual. */
    private boolean compareEquals(String left, String right) {
        Double leftNum = parseOrNull(left);
        Double rightNum = parseOrNull(right);
        if (leftNum != null && rightNum != null) {
            return leftNum.doubleValue() == rightNum.doubleValue();
        }
        return left.equals(right);
    }

    /**
     * Compares numerically when both sides are numbers, otherwise compares as
     * strings, returning the sign of the comparison.
     */
    private int compareNumeric(String left, String right) {
        Double leftNum = parseOrNull(left);
        Double rightNum = parseOrNull(right);
        if (leftNum != null && rightNum != null) {
            return Double.compare(leftNum, rightNum);
        }
        return Integer.signum(left.compareTo(right));
    }

    private static Double parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A human-readable rendering such as {@code count is at least 3}. */
    public String describe() {
        if (comparator.isUnary()) {
            return left + " " + comparator.getLabel();
        }
        return left + " " + comparator.getLabel() + " " + right;
    }

    @Override
    public String toString() {
        return describe();
    }
}
