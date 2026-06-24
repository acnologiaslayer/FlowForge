package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores a value in a named workflow variable so later steps can use it.
 * The value itself may contain {@code ${name}} placeholders that are
 * resolved before being stored.
 */
public class SetVariableTask extends Task {

    private final String variableName;
    private final String value;

    public SetVariableTask(String name, String variableName, String value)
            throws InvalidTaskConfigurationException {
        super(name);
        if (variableName == null || variableName.isBlank()) {
            throw new InvalidTaskConfigurationException("Variable name must not be blank.");
        }
        if (!variableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new InvalidTaskConfigurationException(
                    "Invalid variable name '" + variableName
                            + "'. Use letters, digits and underscores, not starting with a digit.");
        }
        this.variableName = variableName.trim();
        this.value = value == null ? "" : value;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getValue() {
        return value;
    }

    @Override
    public TaskType getType() {
        return TaskType.SET_VARIABLE;
    }

    @Override
    protected String execute(ExecutionContext context) {
        String resolved = context.interpolate(value);
        context.put(variableName, resolved);
        return "Set " + variableName + " = " + resolved;
    }

    @Override
    public String summary() {
        return "Set: " + variableName + " = " + value;
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("variableName", variableName);
        fields.put("value", value);
        return fields;
    }
}
