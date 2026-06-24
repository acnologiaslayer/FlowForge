package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;

import java.util.Map;

/**
 * Abstract base of every workflow step.
 * <p>
 * OOP concepts on show:
 * <ul>
 *   <li><b>Abstraction</b> - the engine runs a list of {@code Task}s without
 *       knowing what each one actually does.</li>
 *   <li><b>Encapsulation</b> - the task name is validated on construction and
 *       cannot be mutated to a blank value afterwards.</li>
 *   <li><b>Polymorphism</b> - {@link #execute(ExecutionContext)} is overridden
 *       by each concrete step.</li>
 *   <li><b>Template method</b> - {@link #run(ExecutionContext)} wraps every
 *       step's {@code execute} in uniform error handling so a low-level
 *       failure always surfaces as a {@link TaskExecutionException}.</li>
 * </ul>
 */
public abstract class Task {

    private String name;

    protected Task(String name) throws InvalidTaskConfigurationException {
        setName(name);
    }

    public final String getName() {
        return name;
    }

    /** Renames the task, enforcing the non-blank rule. */
    public final void setName(String name) throws InvalidTaskConfigurationException {
        if (name == null || name.isBlank()) {
            throw new InvalidTaskConfigurationException("Task name must not be blank.");
        }
        this.name = name.trim();
    }

    public abstract TaskType getType();

    /**
     * Template method: runs the step and guarantees that any failure is
     * reported as a {@link TaskExecutionException} tagged with this task's
     * name. Subclasses implement {@link #execute(ExecutionContext)} instead.
     *
     * @return a human-readable line describing what the step did
     */
    public final String run(ExecutionContext context) throws TaskExecutionException {
        try {
            return execute(context);
        } catch (TaskExecutionException e) {
            throw e; // already tagged with the task name
        } catch (Exception e) {
            throw new TaskExecutionException(name, e.getMessage(), e);
        }
    }

    /** The actual work of the step. Implemented by each concrete task. */
    protected abstract String execute(ExecutionContext context) throws Exception;

    /**
     * A short, single-line summary used in the GUI step list, e.g.
     * "Compute: total = 3 + 4".
     */
    public abstract String summary();

    /**
     * The configuration fields of this task, in a stable order, so the
     * persistence layer can serialise any task generically.
     */
    public abstract Map<String, String> toFields();

    @Override
    public String toString() {
        return getType().getLabel() + " - " + getName();
    }
}
