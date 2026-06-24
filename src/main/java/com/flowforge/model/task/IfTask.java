package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Marks the start of a conditional branch. Together with {@link ElseTask} and
 * {@link EndIfTask} it forms an {@code IF ... ELSE ... END IF} block that the
 * engine compiles into a tree (Composite pattern) and runs as real control
 * flow, exactly like n8n's IF node.
 * <p>
 * The task itself only carries a {@link Condition}; the actual branching is
 * performed by the engine, so {@link #execute(ExecutionContext)} simply
 * reports which way the condition evaluated.
 */
public class IfTask extends Task {

    private final Condition condition;

    public IfTask(String name, Condition condition) throws InvalidTaskConfigurationException {
        super(name);
        if (condition == null) {
            throw new InvalidTaskConfigurationException("An IF step needs a condition.");
        }
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public TaskType getType() {
        return TaskType.IF;
    }

    @Override
    protected String execute(ExecutionContext context) {
        boolean result = condition.evaluate(context);
        return "IF " + condition.describe() + " -> " + (result ? "then" : "else");
    }

    @Override
    public String summary() {
        return "If: " + condition.describe();
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("left", condition.getLeft());
        fields.put("comparator", condition.getComparator().getCode());
        fields.put("right", condition.getRight());
        return fields;
    }
}
