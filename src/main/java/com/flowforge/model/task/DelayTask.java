package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pauses the workflow for a fixed number of milliseconds. Useful for
 * pacing automation steps or simulating long-running work in demos.
 */
public class DelayTask extends Task {

    private final long milliseconds;

    public DelayTask(String name, long milliseconds) throws InvalidTaskConfigurationException {
        super(name);
        if (milliseconds < 0) {
            throw new InvalidTaskConfigurationException(
                    "Delay must not be negative (was " + milliseconds + ").");
        }
        if (milliseconds > 60_000) {
            throw new InvalidTaskConfigurationException(
                    "Delay must not exceed 60000 ms (was " + milliseconds + ").");
        }
        this.milliseconds = milliseconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public TaskType getType() {
        return TaskType.DELAY;
    }

    @Override
    protected String execute(ExecutionContext context) throws TaskExecutionException {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException(getName(), "the run was interrupted while waiting.", e);
        }
        return "Waited " + milliseconds + " ms";
    }

    @Override
    public String summary() {
        return "Delay: " + milliseconds + " ms";
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("milliseconds", String.valueOf(milliseconds));
        return fields;
    }
}
