package com.flowforge.persistence;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.Condition;
import com.flowforge.model.task.ElseTask;
import com.flowforge.model.task.EndIfTask;
import com.flowforge.model.task.EndLoopTask;
import com.flowforge.model.task.HttpRequestTask;
import com.flowforge.model.task.IfTask;
import com.flowforge.model.task.JsonExtractTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.LoopTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.TaskType;
import com.flowforge.model.task.WriteFileTask;
import org.junit.jupiter.api.Test;

import java.util.Map;

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

    @Test
    void roundTripsControlFlowAndIntegrationTasks() throws Exception {
        Workflow original = new Workflow("wf-cf", "Control flow", "");
        original.addStep(new HttpRequestTask("Fetch", HttpRequestTask.Method.POST,
                "https://api.example.com", "{\"q\":1}",
                Map.of("Accept", "application/json"), "resp"));
        original.addStep(new JsonExtractTask("Extract", "${resp_body}", "data.0.id", "id"));
        original.addStep(new IfTask("If",
                new Condition("${id}", Condition.Comparator.GREATER, "0")));
        original.addStep(new LogTask("Then", "positive"));
        original.addStep(new ElseTask("Else"));
        original.addStep(new LogTask("Else body", "non-positive"));
        original.addStep(new EndIfTask("End if"));
        original.addStep(LoopTask.count("Loop", "3", "i"));
        original.addStep(new LogTask("Body", "${i}"));
        original.addStep(new EndLoopTask("End loop"));

        Workflow restored = WorkflowSerializer.deserialize(
                WorkflowSerializer.serialize(original));

        assertEquals(10, restored.stepCount());
        assertEquals(TaskType.HTTP_REQUEST, restored.getStep(0).getType());
        HttpRequestTask http = (HttpRequestTask) restored.getStep(0);
        assertEquals(HttpRequestTask.Method.POST, http.getMethod());
        assertEquals("application/json", http.getHeaders().get("Accept"));
        assertEquals("data.0.id", ((JsonExtractTask) restored.getStep(1)).getPath());
        assertEquals(Condition.Comparator.GREATER,
                ((IfTask) restored.getStep(2)).getCondition().getComparator());
        assertEquals(TaskType.END_IF, restored.getStep(6).getType());
        assertEquals(LoopTask.Mode.COUNT, ((LoopTask) restored.getStep(7)).getMode());
        assertEquals(TaskType.END_LOOP, restored.getStep(9).getType());
    }
}
