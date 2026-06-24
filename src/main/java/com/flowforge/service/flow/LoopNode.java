package com.flowforge.service.flow;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.task.LoopTask;

/**
 * Composite node implementing {@code LOOP ... END LOOP}.
 * <p>
 * Each pass first runs the LOOP marker step (so every iteration is visible in
 * the report) and publishes the current zero-based iteration number into the
 * loop's index variable, then runs the body. Counted loops repeat a fixed
 * number of times; while-loops re-check their condition before each pass. A
 * hard {@link LoopTask#MAX_ITERATIONS} cap protects against runaway loops.
 */
public class LoopNode extends FlowNode {

    private final LoopTask task;
    private final int sourceIndex;
    private final SequenceNode body;

    public LoopNode(LoopTask task, int sourceIndex, SequenceNode body) {
        this.task = task;
        this.sourceIndex = sourceIndex;
        this.body = body;
    }

    public SequenceNode getBody() {
        return body;
    }

    @Override
    public boolean execute(FlowExecution execution) throws WorkflowValidationException {
        return task.getMode() == LoopTask.Mode.COUNT
                ? runCounted(execution)
                : runWhile(execution);
    }

    private boolean runCounted(FlowExecution execution) throws WorkflowValidationException {
        ExecutionContext context = execution.getContext();
        int total;
        try {
            total = task.resolveCount(context);
        } catch (InvalidTaskConfigurationException e) {
            throw new WorkflowValidationException(e.getMessage());
        }
        for (int i = 0; i < total; i++) {
            context.put(task.getIndexVariable(), String.valueOf(i));
            if (!execution.runTask(task, sourceIndex)) {
                return false;
            }
            if (!body.execute(execution)) {
                return false;
            }
        }
        return true;
    }

    private boolean runWhile(FlowExecution execution) throws WorkflowValidationException {
        ExecutionContext context = execution.getContext();
        int iteration = 0;
        while (task.getCondition().evaluate(context)) {
            if (iteration >= LoopTask.MAX_ITERATIONS) {
                throw new WorkflowValidationException(
                        "Loop '" + task.getName() + "' exceeded the safety limit of "
                                + LoopTask.MAX_ITERATIONS + " iterations.");
            }
            context.put(task.getIndexVariable(), String.valueOf(iteration));
            if (!execution.runTask(task, sourceIndex)) {
                return false;
            }
            if (!body.execute(execution)) {
                return false;
            }
            iteration++;
        }
        return true;
    }
}
