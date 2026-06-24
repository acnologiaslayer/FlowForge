package com.flowforge.model.task;

/**
 * The kinds of step a workflow can contain. The {@code code} is the stable
 * token used when a workflow is saved to / loaded from disk, while the
 * {@code label} and {@code description} drive the GUI's task picker.
 */
public enum TaskType {

    LOG("LOG", "Log Message",
            "Print a message to the run log (supports ${variable} placeholders)."),
    SET_VARIABLE("SET", "Set Variable",
            "Store a value in a named variable for later steps to use."),
    COMPUTE("COMPUTE", "Compute",
            "Apply an arithmetic operation to two operands and store the result."),
    DELAY("DELAY", "Delay",
            "Pause the workflow for a number of milliseconds."),
    WRITE_FILE("WRITE_FILE", "Write File",
            "Write text content to a file on disk.");

    private final String code;
    private final String label;
    private final String description;

    TaskType(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** Resolves a persisted code back to its enum constant. */
    public static TaskType fromCode(String code) {
        for (TaskType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type code: " + code);
    }

    public static TaskType fromLabel(String label) {
        for (TaskType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task label: " + label);
    }
}
