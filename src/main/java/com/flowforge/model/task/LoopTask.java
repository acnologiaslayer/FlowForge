package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Marks the start of a loop. Paired with {@link EndLoopTask}, the body between
 * the two markers is executed repeatedly by the engine.
 * <p>
 * Two modes are supported, mirroring n8n's looping building blocks:
 * <ul>
 *   <li>{@link Mode#COUNT} - run the body a fixed number of times;</li>
 *   <li>{@link Mode#WHILE} - run the body while a {@link Condition} holds
 *       (re-checked each pass, with a safety cap to avoid infinite loops).</li>
 * </ul>
 * The current zero-based iteration number is published to the named
 * {@code indexVariable} so the body can use it.
 */
public class LoopTask extends Task {

    /** Whether the loop repeats a fixed number of times or while a condition holds. */
    public enum Mode {
        COUNT, WHILE
    }

    /** Hard cap on WHILE iterations so a bad condition cannot hang a run. */
    public static final int MAX_ITERATIONS = 100_000;

    private final Mode mode;
    private final String count;        // used in COUNT mode (literal or ${var})
    private final Condition condition; // used in WHILE mode
    private final String indexVariable;

    private LoopTask(String name, Mode mode, String count, Condition condition,
                     String indexVariable) throws InvalidTaskConfigurationException {
        super(name);
        if (mode == null) {
            throw new InvalidTaskConfigurationException("Loop mode must not be null.");
        }
        this.mode = mode;
        this.count = count == null ? "" : count.trim();
        this.condition = condition;
        this.indexVariable = (indexVariable == null || indexVariable.isBlank())
                ? "index" : indexVariable.trim();
        if (!this.indexVariable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new InvalidTaskConfigurationException(
                    "Invalid loop index variable '" + this.indexVariable + "'.");
        }
        if (mode == Mode.COUNT && this.count.isBlank()) {
            throw new InvalidTaskConfigurationException("A counted loop needs a count.");
        }
        if (mode == Mode.WHILE && condition == null) {
            throw new InvalidTaskConfigurationException("A while loop needs a condition.");
        }
    }

    /** Factory for a fixed-count loop. */
    public static LoopTask count(String name, String count, String indexVariable)
            throws InvalidTaskConfigurationException {
        return new LoopTask(name, Mode.COUNT, count, null, indexVariable);
    }

    /** Factory for a condition-controlled loop. */
    public static LoopTask whileTrue(String name, Condition condition, String indexVariable)
            throws InvalidTaskConfigurationException {
        return new LoopTask(name, Mode.WHILE, null, condition, indexVariable);
    }

    public Mode getMode() {
        return mode;
    }

    public String getCount() {
        return count;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getIndexVariable() {
        return indexVariable;
    }

    /**
     * Resolves how many times a COUNT loop should run, interpolating and
     * validating the configured count against the current variable state.
     */
    public int resolveCount(ExecutionContext context) throws InvalidTaskConfigurationException {
        String resolved = context.interpolate(count);
        try {
            int value = Integer.parseInt(resolved.trim());
            if (value < 0) {
                throw new InvalidTaskConfigurationException(
                        "Loop count resolved to a negative number: " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidTaskConfigurationException(
                    "Loop count '" + count + "' resolved to '" + resolved
                            + "', which is not a whole number.");
        }
    }

    @Override
    public TaskType getType() {
        return TaskType.LOOP;
    }

    @Override
    protected String execute(ExecutionContext context) {
        // The engine drives the repetition; this line only documents the loop.
        if (mode == Mode.COUNT) {
            return "Loop " + count + " times (index -> " + indexVariable + ")";
        }
        return "Loop while " + condition.describe() + " (index -> " + indexVariable + ")";
    }

    @Override
    public String summary() {
        if (mode == Mode.COUNT) {
            return "Loop: " + count + " times";
        }
        return "Loop while: " + condition.describe();
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mode", mode.name());
        fields.put("indexVariable", indexVariable);
        if (mode == Mode.COUNT) {
            fields.put("count", count);
        } else {
            fields.put("left", condition.getLeft());
            fields.put("comparator", condition.getComparator().getCode());
            fields.put("right", condition.getRight());
        }
        return fields;
    }
}
