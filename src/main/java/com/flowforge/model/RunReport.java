package com.flowforge.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The full result of executing a {@link Workflow}: the per-step outcomes,
 * the final variable state, and overall success. A run stops at the first
 * failing step, so the last {@link StepResult} of a failed run is the one
 * that failed.
 */
public class RunReport {

    private final String workflowName;
    private final List<StepResult> stepResults = new ArrayList<>();
    private boolean success = true;
    private long totalDurationMillis;

    public RunReport(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void add(StepResult result) {
        stepResults.add(result);
        totalDurationMillis += result.getDurationMillis();
        if (!result.isSuccess()) {
            success = false;
        }
    }

    public List<StepResult> getStepResults() {
        return Collections.unmodifiableList(stepResults);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getCompletedSteps() {
        return (int) stepResults.stream().filter(StepResult::isSuccess).count();
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    /** The result of the failing step, or {@code null} if the run succeeded. */
    public StepResult getFailure() {
        if (success || stepResults.isEmpty()) {
            return null;
        }
        StepResult last = stepResults.get(stepResults.size() - 1);
        return last.isSuccess() ? null : last;
    }
}
