package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the concrete {@link Task} types: their validation rules,
 * execution behaviour, variable interpolation, and the template-method
 * error wrapping in {@link Task#run}.
 */
class TaskTest {

    // ---------- name validation (shared via the abstract base) ----------

    @Test
    void blankNameIsRejected() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new LogTask("   ", "hi"));
    }

    @Test
    void nameIsTrimmed() throws Exception {
        LogTask task = new LogTask("  Greet  ", "hi");
        assertEquals("Greet", task.getName());
    }

    // ---------- LogTask ----------

    @Test
    void logTaskInterpolatesVariables() throws Exception {
        ExecutionContext context = new ExecutionContext();
        context.put("user", "Mahir");
        LogTask task = new LogTask("Greet", "Hello ${user}");
        assertEquals("Hello Mahir", task.run(context));
    }

    @Test
    void logTaskRejectsNullMessage() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new LogTask("Greet", null));
    }

    // ---------- SetVariableTask ----------

    @Test
    void setVariableStoresValueInContext() throws Exception {
        ExecutionContext context = new ExecutionContext();
        SetVariableTask task = new SetVariableTask("Set", "count", "5");
        task.run(context);
        assertEquals("5", context.get("count"));
    }

    @Test
    void setVariableRejectsInvalidName() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new SetVariableTask("Set", "1bad", "x"));
    }

    // ---------- ComputeTask ----------

    @Test
    void computeAddsTwoLiterals() throws Exception {
        ExecutionContext context = new ExecutionContext();
        ComputeTask task = new ComputeTask("Add", "sum", "3",
                ComputeTask.Operator.ADD, "4");
        task.run(context);
        assertEquals("7", context.get("sum"));
    }

    @Test
    void computeResolvesVariableOperands() throws Exception {
        ExecutionContext context = new ExecutionContext();
        context.put("a", "10");
        context.put("b", "2");
        ComputeTask task = new ComputeTask("Div", "result", "${a}",
                ComputeTask.Operator.DIVIDE, "${b}");
        task.run(context);
        assertEquals("5", context.get("result"));
    }

    @Test
    void computeDivideByZeroThrowsTaggedException() throws Exception {
        ExecutionContext context = new ExecutionContext();
        ComputeTask task = new ComputeTask("Div", "result", "1",
                ComputeTask.Operator.DIVIDE, "0");
        TaskExecutionException ex = assertThrows(TaskExecutionException.class,
                () -> task.run(context));
        assertEquals("Div", ex.getTaskName());
        assertTrue(ex.getMessage().contains("division by zero"));
    }

    @Test
    void computeNonNumericOperandThrows() throws Exception {
        ExecutionContext context = new ExecutionContext();
        ComputeTask task = new ComputeTask("Add", "result", "notanumber",
                ComputeTask.Operator.ADD, "1");
        assertThrows(TaskExecutionException.class, () -> task.run(context));
    }

    @Test
    void computeRejectsBlankOperands() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new ComputeTask("Add", "r", "", ComputeTask.Operator.ADD, "1"));
    }

    // ---------- DelayTask ----------

    @Test
    void delayRejectsNegativeValue() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new DelayTask("Wait", -1));
    }

    @Test
    void delayRejectsHugeValue() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new DelayTask("Wait", 120_000));
    }

    @Test
    void shortDelayRunsSuccessfully() throws Exception {
        DelayTask task = new DelayTask("Wait", 5);
        String message = task.run(new ExecutionContext());
        assertTrue(message.contains("5 ms"));
    }

    // ---------- template method wrapping ----------

    @Test
    void runWrapsUnexpectedErrorsAsTaskExecutionException() throws Exception {
        // A compute that resolves to a non-number triggers a wrapped failure
        // tagged with the task name, proving Task.run's template behaviour.
        Task task = new ComputeTask("Boom", "r", "x", ComputeTask.Operator.ADD, "1");
        TaskExecutionException ex = assertThrows(TaskExecutionException.class,
                () -> task.run(new ExecutionContext()));
        assertEquals("Boom", ex.getTaskName());
    }
}
