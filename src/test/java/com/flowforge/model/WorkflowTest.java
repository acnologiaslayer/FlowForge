package com.flowforge.model;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Workflow} step management: ordering, moving, removing and
 * bounds checking.
 */
class WorkflowTest {

    private Workflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new Workflow("wf-1", "Test", "desc");
    }

    private Task log(String name) throws InvalidTaskConfigurationException {
        return new LogTask(name, "msg");
    }

    @Test
    void newWorkflowIsEmpty() {
        assertTrue(workflow.isEmpty());
        assertEquals(0, workflow.stepCount());
    }

    @Test
    void stepsKeepInsertionOrder() throws Exception {
        workflow.addStep(log("a"));
        workflow.addStep(log("b"));
        workflow.addStep(log("c"));
        assertEquals("a", workflow.getStep(0).getName());
        assertEquals("c", workflow.getStep(2).getName());
    }

    @Test
    void moveStepUpReordersSteps() throws Exception {
        workflow.addStep(log("a"));
        workflow.addStep(log("b"));
        workflow.moveStepUp(1);
        assertEquals("b", workflow.getStep(0).getName());
        assertEquals("a", workflow.getStep(1).getName());
    }

    @Test
    void moveStepUpAtTopIsNoOp() throws Exception {
        workflow.addStep(log("a"));
        workflow.addStep(log("b"));
        workflow.moveStepUp(0);
        assertEquals("a", workflow.getStep(0).getName());
    }

    @Test
    void moveStepDownReordersSteps() throws Exception {
        workflow.addStep(log("a"));
        workflow.addStep(log("b"));
        workflow.moveStepDown(0);
        assertEquals("b", workflow.getStep(0).getName());
    }

    @Test
    void removeStepReturnsRemovedAndShrinks() throws Exception {
        workflow.addStep(log("a"));
        workflow.addStep(log("b"));
        Task removed = workflow.removeStep(0);
        assertEquals("a", removed.getName());
        assertEquals(1, workflow.stepCount());
    }

    @Test
    void replaceStepSwapsInPlace() throws Exception {
        workflow.addStep(log("a"));
        workflow.replaceStep(0, log("z"));
        assertEquals("z", workflow.getStep(0).getName());
    }

    @Test
    void invalidIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> workflow.getStep(0));
    }

    @Test
    void stepListIsUnmodifiable() throws Exception {
        workflow.addStep(log("a"));
        assertThrows(UnsupportedOperationException.class,
                () -> workflow.getSteps().add(log("b")));
    }

    @Test
    void updatedAtAdvancesOnChange() throws Exception {
        var before = workflow.getUpdatedAt();
        Thread.sleep(2);
        workflow.addStep(log("a"));
        assertTrue(!workflow.getUpdatedAt().isBefore(before));
    }
}
