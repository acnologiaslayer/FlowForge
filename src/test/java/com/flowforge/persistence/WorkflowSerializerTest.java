package com.flowforge.persistence;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.WriteFileTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that {@link WorkflowSerializer} round-trips a workflow and all its
 * task types without losing data, including values that contain the
 * delimiter and newline characters.
 */
class WorkflowSerializerTest {

    @Test
    void roundTripsAllTaskTypes() throws Exception {
        Workflow original = new Workflow("wf-7", "My Flow", "A description");
        original.addStep(new LogTask("Greet", "Hi ${user}"));
        original.addStep(new SetVariableTask("Set", "user", "Mahir"));
        original.addStep(new ComputeTask("Sum", "total", "1",
                ComputeTask.Operator.ADD, "2"));
        original.addStep(new WriteFileTask("Save", "out.txt", "Total: ${total}", true));

        String text = WorkflowSerializer.serialize(original);
        Workflow restored = WorkflowSerializer.deserialize(text);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getDescription(), restored.getDescription());
        assertEquals(4, restored.stepCount());
        assertEquals("Greet", restored.getStep(0).getName());
        assertEquals("Compute", restored.getStep(2).getType().getLabel());
    }

    @Test
    void preservesNewlinesInValues() throws Exception {
        Workflow original = new Workflow("wf-8", "Multi", "");
        original.addStep(new WriteFileTask("Save", "out.txt", "line1\nline2", false));

        Workflow restored = WorkflowSerializer.deserialize(
                WorkflowSerializer.serialize(original));

        WriteFileTask task = (WriteFileTask) restored.getStep(0);
        assertEquals("line1\nline2", task.getContent());
    }

    @Test
    void preservesEqualsSignInValues() throws Exception {
        Workflow original = new Workflow("wf-9", "Eq", "");
        original.addStep(new LogTask("Log", "a=b=c"));

        Workflow restored = WorkflowSerializer.deserialize(
                WorkflowSerializer.serialize(original));

        assertEquals("a=b=c", ((LogTask) restored.getStep(0)).getMessage());
    }

    @Test
    void corruptDataWithoutIdThrows() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> WorkflowSerializer.deserialize("workflow.name=NoId\n"));
    }
}
