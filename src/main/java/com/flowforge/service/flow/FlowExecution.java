package com.flowforge.service.flow;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.Task;
import com.flowforge.service.WorkflowExecutionListener;

/**
 * Carries the mutable state for one walk of a compiled {@link FlowNode} tree:
 * the variable {@link ExecutionContext}, the {@link RunReport} being built,
 * the originating {@link Workflow} and an optional progress listener.
 * <p>
 * Centralising the "run one task" logic here keeps {@link TaskNode} tiny and
 * guarantees every executed step - no matter how deeply nested in IF/LOOP
 * blocks - is timed, recorded in the report and streamed to the listener in a
 * uniform way, using its original position in the flat step list as its index.
 */
public class FlowExecution {

    private final Workflow workflow;
    private final ExecutionContext context;
    private final RunReport report;
    private final WorkflowExecutionListener listener;

    public FlowExecution(Workflow workflow, ExecutionContext context,
                         RunReport report, WorkflowExecutionListener listener) {
        this.workflow = workflow;
        this.context = context;
        this.report = report;
        this.listener = listener;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public RunReport getReport() {
        return report;
    }

    /**
     * Runs a single task, recording the outcome.
     *
     * @param task       the step to run
     * @param sourceIndex the step's position in the flat workflow list
     * @return {@code true} if the step succeeded, {@code false} otherwise
     */
    public boolean runTask(Task task, int sourceIndex) {
        notifyStepStarted(sourceIndex);

        long start = System.nanoTime();
        StepResult result;
        try {
            String message = task.run(context);
            result = StepResult.success(sourceIndex, task, message, millisSince(start));
        } catch (TaskExecutionException e) {
            result = StepResult.failure(sourceIndex, task, e.getMessage(), millisSince(start));
        }

        report.add(result);
        notifyStepFinished(result);
        return result.isSuccess();
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void notifyStepStarted(int index) {
        if (listener != null) {
            listener.onStepStarted(index, workflow);
        }
    }

    private void notifyStepFinished(StepResult result) {
        if (listener != null) {
            listener.onStepFinished(result);
        }
    }
}
