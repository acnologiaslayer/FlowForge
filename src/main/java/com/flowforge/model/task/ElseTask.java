package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structural marker separating the "then" and "else" halves of an
 * {@link IfTask} block. It carries no configuration and performs no work; the
 * engine consumes it while compiling the flat step list into a control-flow
 * tree.
 */
public class ElseTask extends Task {

    public ElseTask(String name) throws InvalidTaskConfigurationException {
        super(name);
    }

    @Override
    public TaskType getType() {
        return TaskType.ELSE;
    }

    @Override
    protected String execute(ExecutionContext context) {
        return "else branch";
    }

    @Override
    public String summary() {
        return "Else";
    }

    @Override
    public Map<String, String> toFields() {
        return new LinkedHashMap<>();
    }
}
