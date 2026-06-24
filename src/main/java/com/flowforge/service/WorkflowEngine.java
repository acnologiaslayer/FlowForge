package com.flowforge.service;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.Workflow;
import com.flowforge.service.flow.FlowExecution;
import com.flowforge.service.flow.FlowParser;
import com.flowforge.service.flow.SequenceNode;

/**
 * Executes a {@link Workflow}.
 * <p>
 * Single responsibility: the engine knows how to <em>run</em> workflows but
 * nothing about how they are stored or displayed. It validates the workflow,
 * compiles its flat step list into a control-flow tree (see
 * {@link FlowParser}), then walks that tree against a shared
 * {@link ExecutionContext}, stopping at the first failure and reporting
 * progress to an optional {@link WorkflowExecutionListener}.
 * <p>
 * Compiling to a tree (the Composite pattern) is what lets the engine support
 * real control flow - IF/ELSE branching and counted/while loops - while the
 * model and persistence layers still deal with a simple ordered list of steps.
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

        SequenceNode program = FlowParser.compile(workflow);

        RunReport report = new RunReport(workflow.getName());
        notifyStarted(listener, workflow);

        FlowExecution execution = new FlowExecution(workflow, context, report, listener);
        program.execute(execution);

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

    // ---------- null-safe listener helpers ----------

    private void notifyStarted(WorkflowExecutionListener l, Workflow w) {
        if (l != null) {
            l.onWorkflowStarted(w);
        }
    }

    private void notifyFinished(WorkflowExecutionListener l, Workflow w, boolean ok) {
        if (l != null) {
            l.onWorkflowFinished(w, ok);
        }
    }
}
