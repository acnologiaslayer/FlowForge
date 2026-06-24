package com.flowforge.service.flow;

import com.flowforge.model.task.Task;

/**
 * Leaf node of the flow tree: wraps one ordinary {@link Task} together with
 * its position in the flat workflow list (so the report and listener keep
 * showing meaningful step numbers even inside nested blocks).
 */
public class TaskNode extends FlowNode {

    private final Task task;
    private final int sourceIndex;

    public TaskNode(Task task, int sourceIndex) {
        this.task = task;
        this.sourceIndex = sourceIndex;
    }

    public Task getTask() {
        return task;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    @Override
    public boolean execute(FlowExecution execution) {
        return execution.runTask(task, sourceIndex);
    }
}
