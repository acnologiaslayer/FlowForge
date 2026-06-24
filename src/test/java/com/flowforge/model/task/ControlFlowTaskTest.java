package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the new n8n-inspired task types that do not require network or
 * timing: {@link JsonExtractTask}, {@link LoopTask} configuration, and the
 * {@link IfTask}/{@link LoopTask} marker behaviour.
 */
class ControlFlowTaskTest {

    // ---------- JsonExtractTask ----------

    @Test
    void jsonExtractPullsNestedValue() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("response_body", "{\"user\":{\"name\":\"Mahir\",\"age\":21}}");
        JsonExtractTask task = new JsonExtractTask("extract",
                "${response_body}", "user.name", "who");
        task.run(ctx);
        assertEquals("Mahir", ctx.get("who"));
    }

    @Test
    void jsonExtractWorksOnInlineJson() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonExtractTask task = new JsonExtractTask("extract",
                "{\"items\":[10,20,30]}", "items.2", "third");
        task.run(ctx);
        assertEquals("30", ctx.get("third"));
    }

    @Test
    void jsonExtractInterpolatesThePath() throws Exception {
        // The path itself can reference variables (e.g. a loop index), so a
        // single extract step can read items.0, items.1, ... across iterations.
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("body", "{\"items\":[\"a\",\"b\",\"c\"]}");
        ctx.put("i", "1");
        JsonExtractTask task = new JsonExtractTask("extract",
                "${body}", "items.${i}", "picked");
        task.run(ctx);
        assertEquals("b", ctx.get("picked"));
    }

    @Test
    void jsonExtractFailsOnBadPath() {
        ExecutionContext ctx = new ExecutionContext();
        JsonExtractTask task = assertDoesNotThrowTask(() -> new JsonExtractTask("extract",
                "{\"a\":1}", "missing", "x"));
        assertThrows(TaskExecutionException.class, () -> task.run(ctx));
    }

    @Test
    void jsonExtractFailsOnInvalidJson() {
        ExecutionContext ctx = new ExecutionContext();
        JsonExtractTask task = assertDoesNotThrowTask(() -> new JsonExtractTask("extract",
                "not json", "a", "x"));
        assertThrows(TaskExecutionException.class, () -> task.run(ctx));
    }

    @Test
    void jsonExtractRejectsBlankResultVariable() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new JsonExtractTask("extract", "{}", "", "  "));
    }

    @Test
    void jsonExtractRoundTripsFields() throws Exception {
        JsonExtractTask task = new JsonExtractTask("extract", "${x}", "a.b", "out");
        assertEquals("${x}", task.toFields().get("source"));
        assertEquals("a.b", task.toFields().get("path"));
        assertEquals("out", task.toFields().get("resultVariable"));
    }

    // ---------- LoopTask configuration ----------

    @Test
    void countLoopResolvesLiteralCount() throws Exception {
        LoopTask loop = LoopTask.count("loop", "5", "i");
        assertEquals(5, loop.resolveCount(new ExecutionContext()));
        assertEquals(LoopTask.Mode.COUNT, loop.getMode());
    }

    @Test
    void countLoopResolvesVariableCount() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("n", "3");
        LoopTask loop = LoopTask.count("loop", "${n}", "i");
        assertEquals(3, loop.resolveCount(ctx));
    }

    @Test
    void countLoopRejectsNonNumericCount() throws Exception {
        LoopTask loop = LoopTask.count("loop", "abc", "i");
        assertThrows(InvalidTaskConfigurationException.class,
                () -> loop.resolveCount(new ExecutionContext()));
    }

    @Test
    void countLoopRejectsNegativeCount() throws Exception {
        LoopTask loop = LoopTask.count("loop", "-1", "i");
        assertThrows(InvalidTaskConfigurationException.class,
                () -> loop.resolveCount(new ExecutionContext()));
    }

    @Test
    void countLoopRequiresCount() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> LoopTask.count("loop", "  ", "i"));
    }

    @Test
    void whileLoopRequiresCondition() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> LoopTask.whileTrue("loop", null, "i"));
    }

    @Test
    void loopRejectsInvalidIndexVariable() throws Exception {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> LoopTask.count("loop", "3", "1bad"));
    }

    @Test
    void loopDefaultsIndexVariableToIndex() throws Exception {
        LoopTask loop = LoopTask.count("loop", "3", "");
        assertEquals("index", loop.getIndexVariable());
    }

    @Test
    void whileLoopRoundTripsConditionFields() throws Exception {
        Condition c = new Condition("${i}", Condition.Comparator.LESS, "5");
        LoopTask loop = LoopTask.whileTrue("loop", c, "i");
        assertEquals("WHILE", loop.toFields().get("mode"));
        assertEquals("<", loop.toFields().get("comparator"));
        assertEquals("5", loop.toFields().get("right"));
    }

    // ---------- markers ----------

    @Test
    void ifTaskReportsBranchTaken() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("x", "10");
        IfTask task = new IfTask("if", new Condition("${x}", Condition.Comparator.GREATER, "5"));
        assertTrue(task.run(ctx).contains("then"));
    }

    @Test
    void markerTasksProduceNoFields() throws Exception {
        assertTrue(new ElseTask("else").toFields().isEmpty());
        assertTrue(new EndIfTask("end").toFields().isEmpty());
        assertTrue(new EndLoopTask("end").toFields().isEmpty());
    }

    @Test
    void markerTasksHaveCorrectTypes() throws Exception {
        assertEquals(TaskType.ELSE, new ElseTask("e").getType());
        assertEquals(TaskType.END_IF, new EndIfTask("e").getType());
        assertEquals(TaskType.END_LOOP, new EndLoopTask("e").getType());
        assertEquals(TaskType.IF, new IfTask("i",
                new Condition("a", Condition.Comparator.EQUALS, "b")).getType());
        assertEquals(TaskType.LOOP, LoopTask.count("l", "1", "i").getType());
    }

    // ---------- HttpRequestTask (offline parts) ----------

    @Test
    void httpHeadersRoundTrip() {
        var headers = java.util.Map.of("Authorization", "Bearer x", "Accept", "application/json");
        String encoded = HttpRequestTask.encodeHeaders(new java.util.LinkedHashMap<>(headers));
        var decoded = HttpRequestTask.decodeHeaders(encoded);
        assertEquals("Bearer x", decoded.get("Authorization"));
        assertEquals("application/json", decoded.get("Accept"));
    }

    @Test
    void httpRejectsBlankUrl() {
        assertThrows(InvalidTaskConfigurationException.class,
                () -> new HttpRequestTask("call", HttpRequestTask.Method.GET, "  ",
                        "", java.util.Map.of(), "response"));
    }

    @Test
    void httpMethodBodyPolicy() {
        assertTrue(HttpRequestTask.Method.POST.allowsBody());
        assertTrue(HttpRequestTask.Method.PUT.allowsBody());
        assertTrue(HttpRequestTask.Method.PATCH.allowsBody());
        assertEquals(false, HttpRequestTask.Method.GET.allowsBody());
    }

    // ---------- helper ----------

    private interface TaskSupplier {
        Task get() throws InvalidTaskConfigurationException;
    }

    private <T extends Task> T assertDoesNotThrowTask(TaskSupplier supplier) {
        try {
            @SuppressWarnings("unchecked")
            T task = (T) supplier.get();
            return task;
        } catch (InvalidTaskConfigurationException e) {
            throw new AssertionError("Task construction should not fail: " + e.getMessage(), e);
        }
    }
}
