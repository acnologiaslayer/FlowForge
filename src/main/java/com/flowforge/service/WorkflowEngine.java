package com.flowforge.service;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.Task;

/**
 * Executes a {@link Workflow} step by step.
 * <p>
 * Single responsibility: the engine knows how to <em>run</em> workflows but
 * nothing about how they are stored or displayed. It validates the workflow,
 * runs each {@link Task} against a shared {@link ExecutionContext}, stops at
 * the first failure, and reports progress to an optional
 * {@link WorkflowExecutionListener}.
 */
public class WorkflowEngine {

    /**
     * Runs the workflow and returns a {@link RunReport}. Execution stops at
     * the first failing step (fail-fast); the report records every step that
     * ran, including the failing one.
     *
     * @param workflow the workflow to run
     * @param context  the variable store shared by the steps
     * @param listener optional progress listener (may be {@code null})
     */
    public RunReport run(Workflow workflow, ExecutionContext context,
                         WorkflowExecutionListener listener)
            throws WorkflowValidationException {
        validate(workflow);

        RunReport report = new RunReport(workflow.getName());
        notifyStarted(listener, workflow);

        for (int i = 0; i < workflow.stepCount(); i++) {
            Task task = workflow.getStep(i);
            notifyStepStarted(listener, i, workflow);

            long start = System.nanoTime();
            StepResult result;
            try {
                String message = task.run(context);
                long elapsed = millisSince(start);
                result = StepResult.success(i, task, message, elapsed);
            } catch (TaskExecutionException e) {
                long elapsed = millisSince(start);
                result = StepResult.failure(i, task, e.getMessage(), elapsed);
            }

            report.add(result);
            notifyStepFinished(listener, result);

            if (!result.isSuccess()) {
                break; // fail fast
            }
        }

        notifyFinished(listener, workflow, report.isSuccess());
        return report;
    }

    /** Convenience overload that supplies a fresh context and no listener. */
    public RunReport run(Workflow workflow) throws WorkflowValidationException {
        return run(workflow, new ExecutionContext(), null);
    }

    private void validate(Workflow workflow) throws WorkflowValidationException {
        if (workflow == null) {
            throw new WorkflowValidationException("Cannot run a null workflow.");
        }
        if (workflow.getName() == null || workflow.getName().isBlank()) {
            throw new WorkflowValidationException("Workflow name must not be blank.");
        }
        if (workflow.isEmpty()) {
            throw new WorkflowValidationException(
                    "Workflow '" + workflow.getName() + "' has no steps to run.");
        }
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    // ---------- null-safe listener helpers ----------

    private void notifyStarted(WorkflowExecutionListener l, Workflow w) {
        if (l != null) {
            l.onWorkflowStarted(w);
        }
    }

    private void notifyStepStarted(WorkflowExecutionListener l, int i, Workflow w) {
        if (l != null) {
            l.onStepStarted(i, w);
        }
    }

    private void notifyStepFinished(WorkflowExecutionListener l, StepResult r) {
        if (l != null) {
            l.onStepFinished(r);
        }
    }

    private void notifyFinished(WorkflowExecutionListener l, Workflow w, boolean ok) {
        if (l != null) {
            l.onWorkflowFinished(w, ok);
        }
    }
}
