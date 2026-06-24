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

    // ---------- new control-flow / integration types ----------

    @Test
    void buildsHttpRequestTask() throws Exception {
        Task task = TaskFactory.create(TaskType.HTTP_REQUEST, "Call", Map.of(
                "method", "POST",
                "url", "https://api.example.com",
                "body", "{}",
                "resultVariable", "resp",
                "headers", "Accept:application/json"));
        assertInstanceOf(HttpRequestTask.class, task);
        HttpRequestTask http = (HttpRequestTask) task;
        assertEquals(HttpRequestTask.Method.POST, http.getMethod());
        assertEquals("application/json", http.getHeaders().get("Accept"));
    }

    @Test
    void buildsJsonExtractTask() throws Exception {
        Task task = TaskFactory.create(TaskType.JSON_EXTRACT, "Extract", Map.of(
                "source", "${body}", "path", "user.name", "resultVariable", "name"));
        assertInstanceOf(JsonExtractTask.class, task);
        assertEquals("user.name", ((JsonExtractTask) task).getPath());
    }

    @Test
    void buildsIfTaskFromCondition() throws Exception {
        Task task = TaskFactory.create(TaskType.IF, "If", Map.of(
                "left", "${x}", "comparator", ">", "right", "5"));
        assertInstanceOf(IfTask.class, task);
        assertEquals(Condition.Comparator.GREATER,
                ((IfTask) task).getCondition().getComparator());
    }

    @Test
    void buildsMarkerTasks() throws Exception {
        assertInstanceOf(ElseTask.class, TaskFactory.create(TaskType.ELSE, "Else", Map.of()));
        assertInstanceOf(EndIfTask.class, TaskFactory.create(TaskType.END_IF, "End", Map.of()));
        assertInstanceOf(EndLoopTask.class,
                TaskFactory.create(TaskType.END_LOOP, "End", Map.of()));
    }

    @Test
    void buildsCountLoop() throws Exception {
        Task task = TaskFactory.create(TaskType.LOOP, "Loop", Map.of(
                "mode", "COUNT", "count", "5", "indexVariable", "i"));
        assertInstanceOf(LoopTask.class, task);
        assertEquals(LoopTask.Mode.COUNT, ((LoopTask) task).getMode());
    }

    @Test
    void buildsWhileLoop() throws Exception {
        Task task = TaskFactory.create(TaskType.LOOP, "Loop", Map.of(
                "mode", "WHILE", "left", "${i}", "comparator", "<", "right", "5",
                "indexVariable", "i"));
        assertEquals(LoopTask.Mode.WHILE, ((LoopTask) task).getMode());
    }

    @Test
    void invalidHttpMethodThrows() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> TaskFactory.create(TaskType.HTTP_REQUEST, "Call", Map.of(
                        "method", "FETCH", "url", "https://x")));
    }

    @Test
    void invalidComparatorThrows() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> TaskFactory.create(TaskType.IF, "If", Map.of(
                        "left", "a", "comparator", "~~", "right", "b")));
    }
}
