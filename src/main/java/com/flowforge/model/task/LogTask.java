package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prints a message to the run log. The message may reference workflow
 * variables using {@code ${name}} placeholders, which are resolved against
 * the {@link ExecutionContext} at run time.
 */
public class LogTask extends Task {

    private final String message;

    public LogTask(String name, String message) throws InvalidTaskConfigurationException {
        super(name);
        if (message == null) {
            throw new InvalidTaskConfigurationException("Log message must not be null.");
        }
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public TaskType getType() {
        return TaskType.LOG;
    }

    @Override
    protected String execute(ExecutionContext context) {
        return context.interpolate(message);
    }

    @Override
    public String summary() {
        return "Log: " + message;
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("message", message);
        return fields;
    }
}
