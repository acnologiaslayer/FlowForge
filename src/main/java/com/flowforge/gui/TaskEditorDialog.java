package com.flowforge.gui;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.DelayTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskType;
import com.flowforge.model.task.WriteFileTask;

import java.awt.Component;
import java.util.Map;

/**
 * Builds and edits {@link Task}s using the themed {@link FormDialog}.
 * <p>
 * It maps each {@link TaskType} to the right set of input fields and turns
 * the entered values into a concrete task, translating any validation error
 * into a friendly {@link MessageDialog}. Keeping this dialog logic out of
 * the main frame keeps {@code FlowForgeApp} focused on layout and wiring.
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

        switch (type) {
            case LOG -> form.addTextArea("message", "Message",
                    existingFields.getOrDefault("message", "Hello from ${name}"));
            case SET_VARIABLE -> form
                    .addText("variableName", "Variable name",
                            existingFields.getOrDefault("variableName", "count"))
                    .addText("value", "Value", existingFields.getOrDefault("value", "1"));
            case COMPUTE -> form
                    .addText("resultVariable", "Result variable",
                            existingFields.getOrDefault("resultVariable", "total"))
                    .addText("leftOperand", "Left operand",
                            existingFields.getOrDefault("leftOperand", "2"))
                    .addCombo("operator", "Operator", new String[]{"+", "-", "*", "/"},
                            existingFields.getOrDefault("operator", "+"))
                    .addText("rightOperand", "Right operand",
                            existingFields.getOrDefault("rightOperand", "3"));
            case DELAY -> form.addText("milliseconds", "Delay (ms)",
                    existingFields.getOrDefault("milliseconds", "500"));
            case WRITE_FILE -> form
                    .addText("path", "File path",
                            existingFields.getOrDefault("path", "data/output.txt"))
                    .addTextArea("content", "Content",
                            existingFields.getOrDefault("content", "Result: ${total}"))
                    .addCheckBox("append", "Append",
                            Boolean.parseBoolean(existingFields.getOrDefault("append", "false")));
        }

        Map<String, String> values = form.showDialog();
        if (values == null) {
            return null;
        }

        try {
            return build(type, values);
        } catch (InvalidTaskConfigurationException e) {
            MessageDialog.show(parent, "Invalid step", e.getMessage(), MessageDialog.Kind.ERROR);
            return null;
        }
    }

    private static Task build(TaskType type, Map<String, String> v)
            throws InvalidTaskConfigurationException {
        String name = v.get("name");
        return switch (type) {
            case LOG -> new LogTask(name, v.getOrDefault("message", ""));
            case SET_VARIABLE -> new SetVariableTask(name,
                    v.get("variableName"), v.getOrDefault("value", ""));
            case COMPUTE -> new ComputeTask(name,
                    v.get("resultVariable"),
                    v.get("leftOperand"),
                    parseOperator(v.get("operator")),
                    v.get("rightOperand"));
            case DELAY -> new DelayTask(name, parseLong(v.get("milliseconds")));
            case WRITE_FILE -> new WriteFileTask(name,
                    v.get("path"),
                    v.getOrDefault("content", ""),
                    Boolean.parseBoolean(v.getOrDefault("append", "false")));
        };
    }

    private static ComputeTask.Operator parseOperator(String symbol)
            throws InvalidTaskConfigurationException {
        try {
            return ComputeTask.Operator.fromSymbol(symbol);
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskConfigurationException("Pick an operator (+ - * /).");
        }
    }

    private static long parseLong(String value) throws InvalidTaskConfigurationException {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTaskConfigurationException(
                    "Delay must be a whole number of milliseconds.");
        }
    }
}
