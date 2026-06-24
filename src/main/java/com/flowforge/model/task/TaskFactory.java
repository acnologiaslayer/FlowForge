package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;

import java.util.Map;

/**
 * Builds concrete {@link Task} instances from a {@link TaskType} and a flat
 * map of fields.
 * <p>
 * Centralising construction here keeps the persistence layer and the GUI
 * free of {@code switch} statements over task types, and means a new task
 * type only has to be wired up in one place (plus its own class).
 */
public final class TaskFactory {

    private TaskFactory() {
    }

    /**
     * Creates a task of the given type from its serialised fields.
     *
     * @param type   the kind of task to build
     * @param name   the task's display name
     * @param fields the configuration produced by {@link Task#toFields()}
     */
    public static Task create(TaskType type, String name, Map<String, String> fields)
            throws InvalidTaskConfigurationException {
        return switch (type) {
            case LOG -> new LogTask(name, fields.getOrDefault("message", ""));
            case SET_VARIABLE -> new SetVariableTask(name,
                    fields.get("variableName"), fields.getOrDefault("value", ""));
            case COMPUTE -> new ComputeTask(name,
                    fields.get("resultVariable"),
                    fields.get("leftOperand"),
                    parseOperator(fields.get("operator")),
                    fields.get("rightOperand"));
            case DELAY -> new DelayTask(name, parseLong(fields.get("milliseconds")));
            case WRITE_FILE -> new WriteFileTask(name,
                    fields.get("path"),
                    fields.getOrDefault("content", ""),
                    Boolean.parseBoolean(fields.getOrDefault("append", "false")));
        };
    }

    private static ComputeTask.Operator parseOperator(String symbol)
            throws InvalidTaskConfigurationException {
        try {
            return ComputeTask.Operator.fromSymbol(symbol);
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskConfigurationException(
                    "Invalid operator '" + symbol + "'. Use one of + - * /.");
        }
    }

    private static long parseLong(String value) throws InvalidTaskConfigurationException {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTaskConfigurationException(
                    "Delay '" + value + "' is not a whole number of milliseconds.");
        }
    }
}
