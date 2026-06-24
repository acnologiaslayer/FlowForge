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
            case HTTP_REQUEST -> new HttpRequestTask(name,
                    parseMethod(fields.get("method")),
                    fields.get("url"),
                    fields.getOrDefault("body", ""),
                    HttpRequestTask.decodeHeaders(fields.get("headers")),
                    fields.getOrDefault("resultVariable", "response"));
            case JSON_EXTRACT -> new JsonExtractTask(name,
                    fields.get("source"),
                    fields.getOrDefault("path", ""),
                    fields.get("resultVariable"));
            case IF -> new IfTask(name, parseCondition(fields));
            case ELSE -> new ElseTask(name);
            case END_IF -> new EndIfTask(name);
            case LOOP -> createLoop(name, fields);
            case END_LOOP -> new EndLoopTask(name);
        };
    }

    private static Task createLoop(String name, Map<String, String> fields)
            throws InvalidTaskConfigurationException {
        LoopTask.Mode mode = parseLoopMode(fields.getOrDefault("mode", "COUNT"));
        String indexVariable = fields.getOrDefault("indexVariable", "index");
        if (mode == LoopTask.Mode.WHILE) {
            return LoopTask.whileTrue(name, parseCondition(fields), indexVariable);
        }
        return LoopTask.count(name, fields.get("count"), indexVariable);
    }

    private static Condition parseCondition(Map<String, String> fields)
            throws InvalidTaskConfigurationException {
        return new Condition(
                fields.get("left"),
                parseComparator(fields.get("comparator")),
                fields.getOrDefault("right", ""));
    }

    private static HttpRequestTask.Method parseMethod(String value)
            throws InvalidTaskConfigurationException {
        try {
            return HttpRequestTask.Method.valueOf(value == null ? "GET" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskConfigurationException(
                    "Invalid HTTP method '" + value + "'. Use GET, POST, PUT, DELETE or PATCH.");
        }
    }

    private static Condition.Comparator parseComparator(String code)
            throws InvalidTaskConfigurationException {
        try {
            return Condition.Comparator.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskConfigurationException(
                    "Invalid condition comparator '" + code + "'.");
        }
    }

    private static LoopTask.Mode parseLoopMode(String value)
            throws InvalidTaskConfigurationException {
        try {
            return LoopTask.Mode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskConfigurationException(
                    "Invalid loop mode '" + value + "'. Use COUNT or WHILE.");
        }
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
