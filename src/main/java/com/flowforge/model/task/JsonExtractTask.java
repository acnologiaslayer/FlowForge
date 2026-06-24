package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.JsonException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.json.JsonParser;
import com.flowforge.model.json.JsonPath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a JSON string and pulls a value out of it using a dot/bracket path,
 * storing the result in a variable. This is the data-shaping companion to
 * {@link HttpRequestTask}: fetch a payload, then extract the field you need,
 * just like wiring an HTTP node into a Set/Edit-Fields node in n8n.
 * <p>
 * The {@code source} is interpolated first, so it can reference an earlier
 * response (e.g. {@code ${response_body}}) or contain inline JSON.
 */
public class JsonExtractTask extends Task {

    private final String source;
    private final String path;
    private final String resultVariable;

    public JsonExtractTask(String name, String source, String path, String resultVariable)
            throws InvalidTaskConfigurationException {
        super(name);
        if (source == null || source.isBlank()) {
            throw new InvalidTaskConfigurationException(
                    "JSON source must not be blank (use ${variable} or inline JSON).");
        }
        if (resultVariable == null || resultVariable.isBlank()) {
            throw new InvalidTaskConfigurationException("Result variable must not be blank.");
        }
        if (!resultVariable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new InvalidTaskConfigurationException(
                    "Invalid result variable '" + resultVariable + "'.");
        }
        this.source = source.trim();
        this.path = path == null ? "" : path.trim();
        this.resultVariable = resultVariable.trim();
    }

    public String getSource() {
        return source;
    }

    public String getPath() {
        return path;
    }

    public String getResultVariable() {
        return resultVariable;
    }

    @Override
    public TaskType getType() {
        return TaskType.JSON_EXTRACT;
    }

    @Override
    protected String execute(ExecutionContext context) throws TaskExecutionException {
        String json = context.interpolate(source);
        String resolvedPath = context.interpolate(path);
        try {
            Object tree = JsonParser.parse(json);
            String value = JsonPath.read(tree, resolvedPath);
            context.put(resultVariable, value);
            String shown = value.length() > 60 ? value.substring(0, 57) + "..." : value;
            return "Extracted " + (resolvedPath.isBlank() ? "<root>" : resolvedPath)
                    + " -> " + resultVariable + " = " + shown;
        } catch (JsonException e) {
            throw new TaskExecutionException(getName(), e.getMessage(), e);
        }
    }

    @Override
    public String summary() {
        return "JSON Extract: " + (path.isBlank() ? "<root>" : path) + " -> " + resultVariable;
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("source", source);
        fields.put("path", path);
        fields.put("resultVariable", resultVariable);
        return fields;
    }
}
