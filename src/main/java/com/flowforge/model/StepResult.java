package com.flowforge.model;

import com.flowforge.model.task.Task;

/**
 * The outcome of running a single {@link Task}: which task ran, whether it
 * succeeded, the message it produced (or the error), and how long it took.
 */
public class StepResult {

    private final int index;
    private final String taskName;
    private final String taskType;
    private final boolean success;
    private final String message;
    private final long durationMillis;

    private StepResult(int index, String taskName, String taskType,
                       boolean success, String message, long durationMillis) {
        this.index = index;
        this.taskName = taskName;
        this.taskType = taskType;
        this.success = success;
        this.message = message;
        this.durationMillis = durationMillis;
    }

    public static StepResult success(int index, Task task, String message, long durationMillis) {
        return new StepResult(index, task.getName(), task.getType().getLabel(),
                true, message, durationMillis);
    }

    public static StepResult failure(int index, Task task, String message, long durationMillis) {
        return new StepResult(index, task.getName(), task.getType().getLabel(),
                false, message, durationMillis);
    }

    public int getIndex() {
        return index;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    @Override
    public String toString() {
        return String.format("[%s] step %d - %s (%s): %s (%d ms)",
                success ? "OK" : "FAIL", index + 1, taskName, taskType, message, durationMillis);
    }
}
