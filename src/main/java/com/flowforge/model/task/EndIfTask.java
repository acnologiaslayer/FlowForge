package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structural marker closing an {@link IfTask} block. It carries no
 * configuration and performs no work; the engine consumes it while compiling
 * the flat step list into a control-flow tree.
 */
public class EndIfTask extends Task {

    public EndIfTask(String name) throws InvalidTaskConfigurationException {
        super(name);
    }

    @Override
    public TaskType getType() {
        return TaskType.END_IF;
    }

    @Override
    protected String execute(ExecutionContext context) {
        return "end if";
    }

    @Override
    public String summary() {
        return "End If";
    }

    @Override
    public Map<String, String> toFields() {
        return new LinkedHashMap<>();
    }
}
