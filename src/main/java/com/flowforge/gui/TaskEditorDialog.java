package com.flowforge.gui;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.task.Condition;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskFactory;
import com.flowforge.model.task.TaskType;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds and edits {@link Task}s using the themed {@link FormDialog}.
 * <p>
 * It maps each {@link TaskType} to the right set of input fields, then hands
 * the entered values to {@link TaskFactory} to construct the task, translating
 * any validation error into a friendly {@link MessageDialog}. Routing
 * construction through the factory means this dialog never duplicates the
 * task-building logic and a new task type only needs a form description here.
 */
public final class TaskEditorDialog {

    private TaskEditorDialog() {
    }

    /**
     * Shows a dialog to create a task of {@code type}.
     *
     * @return the new task, or {@code null} if the user cancelled
     */
    public static Task createTask(Component parent, TaskType type) {
        return showForm(parent, type, null);
    }

    /**
     * Shows a dialog pre-filled with an existing task's values for editing.
     *
     * @return the updated task, or {@code null} if cancelled
     */
    public static Task editTask(Component parent, Task existing) {
        return showForm(parent, existing.getType(), existing);
    }

    private static Task showForm(Component parent, TaskType type, Task existing) {
        Map<String, String> existingFields = existing == null ? Map.of() : existing.toFields();
        String existingName = existing == null ? type.getLabel() : existing.getName();

        FormDialog form = new FormDialog(parent,
                (existing == null ? "Add " : "Edit ") + type.getLabel());
        form.addText("name", "Step name", existingName);

        describeFields(form, type, existingFields);

        Map<String, String> values = form.showDialog();
        if (values == null) {
            return null;
        }

        try {
            return TaskFactory.create(type, values.get("name"), toTaskFields(type, values));
        } catch (InvalidTaskConfigurationException e) {
            MessageDialog.show(parent, "Invalid step", e.getMessage(), MessageDialog.Kind.ERROR);
            return null;
        }
    }

    /**
     * Adds the type-specific input fields to {@code form}. Marker steps
     * (Else / End If / End Loop) contribute no fields.
     */
    private static void describeFields(FormDialog form, TaskType type,
                                       Map<String, String> existing) {
        switch (type) {
            case LOG -> form.addTextArea("message", "Message",
                    existing.getOrDefault("message", "Hello from ${name}"));
            case SET_VARIABLE -> form
                    .addText("variableName", "Variable name",
                            existing.getOrDefault("variableName", "count"))
                    .addText("value", "Value", existing.getOrDefault("value", "1"));
            case COMPUTE -> form
                    .addText("resultVariable", "Result variable",
                            existing.getOrDefault("resultVariable", "total"))
                    .addText("leftOperand", "Left operand",
                            existing.getOrDefault("leftOperand", "2"))
                    .addCombo("operator", "Operator", new String[]{"+", "-", "*", "/"},
                            existing.getOrDefault("operator", "+"))
                    .addText("rightOperand", "Right operand",
                            existing.getOrDefault("rightOperand", "3"));
            case DELAY -> form.addText("milliseconds", "Delay (ms)",
                    existing.getOrDefault("milliseconds", "500"));
            case WRITE_FILE -> form
                    .addText("path", "File path",
                            existing.getOrDefault("path", "data/output.txt"))
                    .addTextArea("content", "Content",
                            existing.getOrDefault("content", "Result: ${total}"))
                    .addCheckBox("append", "Append",
                            Boolean.parseBoolean(existing.getOrDefault("append", "false")));
            case HTTP_REQUEST -> form
                    .addCombo("method", "Method",
                            new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"},
                            existing.getOrDefault("method", "GET"))
                    .addText("url", "URL",
                            existing.getOrDefault("url", "https://api.example.com/data"))
                    .addTextArea("headers", "Headers (key:value per line)",
                            existing.getOrDefault("headers", ""))
                    .addTextArea("body", "Body", existing.getOrDefault("body", ""))
                    .addText("resultVariable", "Store response in",
                            existing.getOrDefault("resultVariable", "response"));
            case JSON_EXTRACT -> form
                    .addTextArea("source", "JSON source",
                            existing.getOrDefault("source", "${response_body}"))
                    .addText("path", "Path (e.g. data.items.0.name)",
                            existing.getOrDefault("path", ""))
                    .addText("resultVariable", "Store value in",
                            existing.getOrDefault("resultVariable", "value"));
            case IF -> addConditionFields(form, existing, "value");
            case LOOP -> {
                form.addCombo("mode", "Mode", new String[]{"COUNT", "WHILE"},
                        existing.getOrDefault("mode", "COUNT"));
                form.addText("count", "Count (COUNT mode)",
                        existing.getOrDefault("count", "3"));
                form.addText("indexVariable", "Index variable",
                        existing.getOrDefault("indexVariable", "index"));
                addConditionFields(form, existing, "index");
            }
            case ELSE, END_IF, END_LOOP -> {
                // marker steps have no configuration
            }
        }
    }

    /** Adds the shared left/comparator/right inputs used by IF and WHILE loops. */
    private static void addConditionFields(FormDialog form, Map<String, String> existing,
                                           String defaultLeft) {
        form.addText("left", "Left value", existing.getOrDefault("left", "${" + defaultLeft + "}"))
                .addCombo("comparator", "Comparator", comparatorLabels(),
                        labelForCode(existing.getOrDefault("comparator", "==")))
                .addText("right", "Right value", existing.getOrDefault("right", "1"));
    }

    /**
     * Converts the raw form values into the flat field map that
     * {@link TaskFactory} expects (mainly mapping the comparator label chosen
     * in the combo box back to its stable code).
     */
    private static Map<String, String> toTaskFields(TaskType type, Map<String, String> values) {
        Map<String, String> fields = new LinkedHashMap<>(values);
        fields.remove("name");
        if (fields.containsKey("comparator")) {
            fields.put("comparator", codeForLabel(fields.get("comparator")));
        }
        return fields;
    }

    // ---------- comparator label <-> code mapping ----------

    private static String[] comparatorLabels() {
        Condition.Comparator[] values = Condition.Comparator.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].getLabel();
        }
        return labels;
    }

    private static String labelForCode(String code) {
        try {
            return Condition.Comparator.fromCode(code).getLabel();
        } catch (IllegalArgumentException e) {
            return Condition.Comparator.EQUALS.getLabel();
        }
    }

    private static String codeForLabel(String label) {
        for (Condition.Comparator c : Condition.Comparator.values()) {
            if (c.getLabel().equals(label)) {
                return c.getCode();
            }
        }
        return Condition.Comparator.EQUALS.getCode();
    }
}
