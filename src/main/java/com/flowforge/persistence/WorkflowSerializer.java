package com.flowforge.persistence;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskFactory;
import com.flowforge.model.task.TaskType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a {@link Workflow} to and from a simple, human-readable text
 * format, with no third-party libraries.
 * <p>
 * Format (one key/value pair per line; values are escaped so newlines and
 * separators survive a round trip):
 * <pre>
 * workflow.id=wf-1
 * workflow.name=My Flow
 * workflow.description=...
 * workflow.createdAt=...   (epoch millis)
 * workflow.updatedAt=...
 * step.0.type=COMPUTE
 * step.0.name=Add totals
 * step.0.field.resultVariable=total
 * ...
 * </pre>
 */
public final class WorkflowSerializer {

    private static final String NL = "\n";

    private WorkflowSerializer() {
    }

    /** Serialises a workflow to the text format described above. */
    public static String serialize(Workflow workflow) {
        StringBuilder sb = new StringBuilder();
        line(sb, "workflow.id", workflow.getId());
        line(sb, "workflow.name", workflow.getName());
        line(sb, "workflow.description", workflow.getDescription());
        line(sb, "workflow.createdAt", String.valueOf(workflow.getCreatedAt().toEpochMilli()));
        line(sb, "workflow.updatedAt", String.valueOf(workflow.getUpdatedAt().toEpochMilli()));

        List<Task> steps = workflow.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            Task task = steps.get(i);
            line(sb, "step." + i + ".type", task.getType().getCode());
            line(sb, "step." + i + ".name", task.getName());
            for (Map.Entry<String, String> field : task.toFields().entrySet()) {
                line(sb, "step." + i + ".field." + field.getKey(), field.getValue());
            }
        }
        return sb.toString();
    }

    /** Parses the text produced by {@link #serialize(Workflow)}. */
    public static Workflow deserialize(String text) throws InvalidTaskConfigurationException {
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : text.split("\n", -1)) {
            if (raw.isBlank()) {
                continue;
            }
            int eq = raw.indexOf('=');
            if (eq < 0) {
                continue;
            }
            values.put(raw.substring(0, eq), unescape(raw.substring(eq + 1)));
        }

        String id = values.get("workflow.id");
        String name = values.getOrDefault("workflow.name", "Untitled");
        String description = values.getOrDefault("workflow.description", "");
        Instant createdAt = parseInstant(values.get("workflow.createdAt"));
        Instant updatedAt = parseInstant(values.get("workflow.updatedAt"));

        if (id == null || id.isBlank()) {
            throw new InvalidTaskConfigurationException("Stored workflow is missing its id.");
        }

        Workflow workflow = new Workflow(id, name, description, createdAt, updatedAt);

        int index = 0;
        while (values.containsKey("step." + index + ".type")) {
            String prefix = "step." + index + ".";
            TaskType type = TaskType.fromCode(values.get(prefix + "type"));
            String stepName = values.getOrDefault(prefix + "name", type.getLabel());

            Map<String, String> fields = new LinkedHashMap<>();
            String fieldPrefix = prefix + "field.";
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey().startsWith(fieldPrefix)) {
                    fields.put(entry.getKey().substring(fieldPrefix.length()), entry.getValue());
                }
            }
            workflow.addStep(TaskFactory.create(type, stepName, fields));
            index++;
        }
        return workflow;
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append('=').append(escape(value)).append(NL);
    }

    /** Escapes backslashes and newlines so a value stays on one line. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "");
    }

    private static String unescape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                sb.append(next == 'n' ? '\n' : next);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Instant parseInstant(String millis) {
        if (millis == null || millis.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(millis.trim()));
        } catch (NumberFormatException e) {
            return Instant.now();
        }
    }
}
