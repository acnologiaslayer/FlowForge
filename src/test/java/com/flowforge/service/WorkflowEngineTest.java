package com.flowforge.service;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WorkflowEngine}: end-to-end runs, variable propagation
 * between steps, fail-fast behaviour, validation, and listener callbacks.
 */
class WorkflowEngineTest {

    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new WorkflowEngine();
    }

    private Workflow workflow() {
        return new Workflow("wf-1", "Flow", "");
    }

    @Test
    void runsAllStepsAndPropagatesVariables() throws Exception {
        Workflow workflow = workflow();
        workflow.addStep(new SetVariableTask("a", "morning", "100"));
        workflow.addStep(new SetVariableTask("b", "evening", "150"));
        workflow.addStep(new ComputeTask("sum", "total", "${morning}",
                ComputeTask.Operator.ADD, "${evening}"));

        ExecutionContext context = new ExecutionContext();
        RunReport report = engine.run(workflow, context, null);

        assertTrue(report.isSuccess());
        assertEquals(3, report.getCompletedSteps());
        assertEquals("250", context.get("total"));
    }

    @Test
    void runStopsAtFirstFailure() throws Exception {
        Workflow workflow = workflow();
        workflow.addStep(new LogTask("ok", "fine"));
        workflow.addStep(new ComputeTask("bad", "x", "1",
                ComputeTask.Operator.DIVIDE, "0"));
        workflow.addStep(new LogTask("never", "should not run"));

        RunReport report = engine.run(workflow);

        assertFalse(report.isSuccess());
        assertEquals(1, report.getCompletedSteps());
        assertEquals(2, report.getStepResults().size()); // ok + the failing one
        StepResult failure = report.getFailure();
        assertNotNull(failure);
        assertEquals("bad", failure.getTaskName());
    }

    @Test
    void emptyWorkflowFailsValidation() {
        assertThrows(WorkflowValidationException.class, () -> engine.run(workflow()));
    }

    @Test
    void blankNameFailsValidation() throws Exception {
        Workflow workflow = new Workflow("wf-1", "  ", "");
        workflow.addStep(new LogTask("a", "x"));
        assertThrows(WorkflowValidationException.class, () -> engine.run(workflow));
    }

    @Test
    void nullWorkflowFailsValidation() {
        assertThrows(WorkflowValidationException.class, () -> engine.run(null));
    }

    @Test
    void listenerReceivesAllCallbacks() throws Exception {
        Workflow workflow = workflow();
        workflow.addStep(new LogTask("a", "x"));
        workflow.addStep(new LogTask("b", "y"));

        List<String> events = new ArrayList<>();
        engine.run(workflow, new ExecutionContext(), new WorkflowExecutionListener() {
            @Override
            public void onWorkflowStarted(Workflow w) {
                events.add("started");
            }

            @Override
            public void onStepStarted(int index, Workflow w) {
                events.add("step-start-" + index);
            }

            @Override
            public void onStepFinished(StepResult result) {
                events.add("step-done-" + result.getIndex());
            }

            @Override
            public void onWorkflowFinished(Workflow w, boolean success) {
                events.add("finished-" + success);
            }
        });

        assertEquals(List.of("started", "step-start-0", "step-done-0",
                "step-start-1", "step-done-1", "finished-true"), events);
    }

    @Test
    void reportTracksTotalDuration() throws Exception {
        Workflow workflow = workflow();
        workflow.addStep(new LogTask("a", "x"));
        RunReport report = engine.run(workflow);
        assertTrue(report.getTotalDurationMillis() >= 0);
    }
}
