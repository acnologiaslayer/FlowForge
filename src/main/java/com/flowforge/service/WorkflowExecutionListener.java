package com.flowforge.service;

import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;

/**
 * Receives live notifications while a {@link Workflow} runs, so the GUI can
 * stream progress into its run log instead of waiting for the whole run to
 * finish.
 * <p>
 * It is an interface with default no-op methods, so a listener only needs
 * to override the callbacks it cares about (an example of programming to an
 * abstraction rather than a concrete UI class).
 */
public interface WorkflowExecutionListener {

    /** Called once before the first step runs. */
    default void onWorkflowStarted(Workflow workflow) {
    }

    /** Called immediately before a step is executed. */
    default void onStepStarted(int index, Workflow workflow) {
    }

    /** Called after each step, whether it succeeded or failed. */
    default void onStepFinished(StepResult result) {
    }

    /** Called once after the run ends (successfully or not). */
    default void onWorkflowFinished(Workflow workflow, boolean success) {
    }
}
