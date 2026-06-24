package com.flowforge.model;

import com.flowforge.model.task.Task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered collection of {@link Task}s that runs top to bottom.
 * <p>
 * Encapsulation: the step list is private and only mutated through the
 * methods here, which keep the move/remove operations bounds-safe and bump
 * the {@code updatedAt} timestamp so the UI can show when a workflow last
 * changed.
 */
public class Workflow {

    private final String id;
    private String name;
    private String description;
    private final List<Task> steps = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    public Workflow(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Constructor used when reloading from disk, preserving timestamps. */
    public Workflow(String id, String name, String description,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ---------- step management ----------

    public void addStep(Task task) {
        steps.add(task);
        touch();
    }

    public void insertStep(int index, Task task) {
        steps.add(clampInsert(index), task);
        touch();
    }

    public Task removeStep(int index) {
        Task removed = steps.remove(requireValidIndex(index));
        touch();
        return removed;
    }

    public void replaceStep(int index, Task task) {
        steps.set(requireValidIndex(index), task);
        touch();
    }

    /** Moves the step at {@code index} one position earlier; no-op at the top. */
    public void moveStepUp(int index) {
        requireValidIndex(index);
        if (index > 0) {
            Collections.swap(steps, index, index - 1);
            touch();
        }
    }

    /** Moves the step at {@code index} one position later; no-op at the bottom. */
    public void moveStepDown(int index) {
        requireValidIndex(index);
        if (index < steps.size() - 1) {
            Collections.swap(steps, index, index + 1);
            touch();
        }
    }

    public Task getStep(int index) {
        return steps.get(requireValidIndex(index));
    }

    public int stepCount() {
        return steps.size();
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    /** A read-only view of the steps, in execution order. */
    public List<Task> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private int requireValidIndex(int index) {
        if (index < 0 || index >= steps.size()) {
            throw new IndexOutOfBoundsException(
                    "Step index " + index + " out of range [0, " + steps.size() + ").");
        }
        return index;
    }

    private int clampInsert(int index) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, steps.size());
    }

    @Override
    public String toString() {
        return name + " (" + steps.size() + " step" + (steps.size() == 1 ? "" : "s") + ")";
    }
}
