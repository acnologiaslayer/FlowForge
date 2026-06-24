package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link TaskFactory}: building each concrete task from a flat
 * field map, and rejecting malformed configuration. This mirrors how the
 * persistence layer reconstructs tasks loaded from disk.
 */
class TaskFactoryTest {

    @Test
    void buildsLogTask() throws Exception {
        Task task = TaskFactory.create(TaskType.LOG, "Log", Map.of("message", "hi"));
        assertInstanceOf(LogTask.class, task);
        assertEquals("hi", ((LogTask) task).getMessage());
    }

    @Test
    void buildsComputeTaskWithOperator() throws Exception {
        Task task = TaskFactory.create(TaskType.COMPUTE, "Sum", Map.of(
                "resultVariable", "total",
                "leftOperand", "2",
                "operator", "*",
                "rightOperand", "3"));
        assertInstanceOf(ComputeTask.class, task);
        assertEquals(ComputeTask.Operator.MULTIPLY, ((ComputeTask) task).getOperator());
    }

    @Test
    void buildsDelayTask() throws Exception {
        Task task = TaskFactory.create(TaskType.DELAY, "Wait",
                Map.of("milliseconds", "250"));
        assertInstanceOf(DelayTask.class, task);
        assertEquals(250, ((DelayTask) task).getMilliseconds());
    }

    @Test
    void buildsWriteFileTask() throws Exception {
        Task task = TaskFactory.create(TaskType.WRITE_FILE, "Save", Map.of(
                "path", "out.txt", "content", "data", "append", "true"));
        assertInstanceOf(WriteFileTask.class, task);
        assertEquals(true, ((WriteFileTask) task).isAppend());
    }

    @Test
    void invalidOperatorThrows() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> TaskFactory.create(TaskType.COMPUTE, "Sum", Map.of(
                        "resultVariable", "r", "leftOperand", "1",
                        "operator", "%", "rightOperand", "2")));
    }

    @Test
    void invalidDelayThrows() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> TaskFactory.create(TaskType.DELAY, "Wait",
                        Map.of("milliseconds", "soon")));
    }

    @Test
    void taskTypeCodeRoundTrips() {
        for (TaskType type : TaskType.values()) {
            assertEquals(type, TaskType.fromCode(type.getCode()));
        }
    }

    @Test
    void unknownTaskCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> TaskType.fromCode("BOGUS"));
    }
}
