package com.flowforge.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The shared, mutable state passed from one {@link com.flowforge.model.task.Task}
 * to the next while a workflow runs.
 * <p>
 * Encapsulation: tasks never touch each other directly; they only read and
 * write named variables here, which keeps steps decoupled and lets a step
 * consume the output of an earlier one (e.g. a compute step storing a total
 * that a later log step prints).
 */
public class ExecutionContext {

    private final Map<String, String> variables = new LinkedHashMap<>();

    /** Stores (or overwrites) a variable. A {@code null} value is stored as "". */
    public void put(String key, String value) {
        variables.put(key, value == null ? "" : value);
    }

    /** Returns the raw value, or {@code null} if the variable is unset. */
    public String get(String key) {
        return variables.get(key);
    }

    public boolean contains(String key) {
        return variables.containsKey(key);
    }

    /**
     * Replaces every {@code ${name}} placeholder in {@code template} with the
     * matching variable value (unknown names are left untouched), so tasks can
     * build messages and file contents from earlier results.
     */
    public String interpolate(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /** A read-only snapshot of all variables, for display in the UI. */
    public Map<String, String> snapshot() {
        return new LinkedHashMap<>(variables);
    }

    public void clear() {
        variables.clear();
    }
}
