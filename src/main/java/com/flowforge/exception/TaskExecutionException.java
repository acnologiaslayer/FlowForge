package com.flowforge.exception;

/**
 * Thrown when an individual {@code Task} fails while a workflow is running
 * (for example a division by zero in a compute step or a file that cannot
 * be written). Carries the name of the offending task so the engine can
 * report exactly where a run stopped.
 */
public class TaskExecutionException extends WorkflowException {

    private final String taskName;

    public TaskExecutionException(String taskName, String message) {
        super("Task '" + taskName + "' failed: " + message);
        this.taskName = taskName;
    }

    public TaskExecutionException(String taskName, String message, Throwable cause) {
        super("Task '" + taskName + "' failed: " + message, cause);
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }
}
