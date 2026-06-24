package com.flowforge.service;

import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.Condition;
import com.flowforge.model.task.ElseTask;
import com.flowforge.model.task.EndIfTask;
import com.flowforge.model.task.EndLoopTask;
import com.flowforge.model.task.IfTask;
import com.flowforge.model.task.JsonExtractTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.LoopTask;
import com.flowforge.model.task.SetVariableTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the control-flow features running through the real
 * {@link WorkflowEngine}: IF/ELSE branching, counted and while loops, nesting
 * and the JSON-extract data path.
 */
class ControlFlowEngineTest {

    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new WorkflowEngine();
    }

    private Workflow workflow() {
        return new Workflow("wf", "Flow", "");
    }

    @Test
    void ifTakesThenBranchWhenTrue() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("set", "x", "10"));
        wf.addStep(new IfTask("if", new Condition("${x}", Condition.Comparator.GREATER, "5")));
        wf.addStep(new SetVariableTask("then", "result", "big"));
        wf.addStep(new ElseTask("else"));
        wf.addStep(new SetVariableTask("else-body", "result", "small"));
        wf.addStep(new EndIfTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        assertTrue(engine.run(wf, ctx, null).isSuccess());
        assertEquals("big", ctx.get("result"));
    }

    @Test
    void ifTakesElseBranchWhenFalse() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("set", "x", "2"));
        wf.addStep(new IfTask("if", new Condition("${x}", Condition.Comparator.GREATER, "5")));
        wf.addStep(new SetVariableTask("then", "result", "big"));
        wf.addStep(new ElseTask("else"));
        wf.addStep(new SetVariableTask("else-body", "result", "small"));
        wf.addStep(new EndIfTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        assertEquals("small", ctx.get("result"));
    }

    @Test
    void thenBranchStepsAreSkippedOnElsePath() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new IfTask("if", new Condition("1", Condition.Comparator.EQUALS, "2")));
        wf.addStep(new LogTask("then", "should be skipped"));
        wf.addStep(new EndIfTask("end"));

        RunReport report = engine.run(wf);
        // Only the IF marker runs; the then-body is skipped.
        assertEquals(1, report.getStepResults().size());
        assertTrue(report.isSuccess());
    }

    @Test
    void countedLoopRunsBodyNTimes() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("init", "sum", "0"));
        wf.addStep(LoopTask.count("loop", "3", "i"));
        wf.addStep(new ComputeTask("add", "sum", "${sum}", ComputeTask.Operator.ADD, "10"));
        wf.addStep(new EndLoopTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        assertEquals("30", ctx.get("sum"));
        assertEquals("2", ctx.get("i")); // last index seen (0,1,2)
    }

    @Test
    void zeroCountLoopRunsBodyZeroTimes() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("init", "sum", "5"));
        wf.addStep(LoopTask.count("loop", "0", "i"));
        wf.addStep(new ComputeTask("add", "sum", "${sum}", ComputeTask.Operator.ADD, "10"));
        wf.addStep(new EndLoopTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        assertEquals("5", ctx.get("sum"));
    }

    @Test
    void whileLoopRunsUntilConditionFails() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("init", "n", "0"));
        wf.addStep(LoopTask.whileTrue("loop",
                new Condition("${n}", Condition.Comparator.LESS, "5"), "i"));
        wf.addStep(new ComputeTask("inc", "n", "${n}", ComputeTask.Operator.ADD, "1"));
        wf.addStep(new EndLoopTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        assertEquals("5", ctx.get("n"));
    }

    @Test
    void nestedLoopAndIfWork() throws Exception {
        // For i in 0..3: if i is even-ish (>=2) add 100 else add 1
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("init", "total", "0"));
        wf.addStep(LoopTask.count("loop", "4", "i"));
        wf.addStep(new IfTask("if",
                new Condition("${i}", Condition.Comparator.GREATER_OR_EQUAL, "2")));
        wf.addStep(new ComputeTask("big", "total", "${total}", ComputeTask.Operator.ADD, "100"));
        wf.addStep(new ElseTask("else"));
        wf.addStep(new ComputeTask("small", "total", "${total}", ComputeTask.Operator.ADD, "1"));
        wf.addStep(new EndIfTask("endif"));
        wf.addStep(new EndLoopTask("endloop"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        // i=0,1 -> +1 each = 2 ; i=2,3 -> +100 each = 200 ; total 202
        assertEquals("202", ctx.get("total"));
    }

    @Test
    void failureInsideLoopBodyStopsRun() throws Exception {
        Workflow wf = workflow();
        wf.addStep(LoopTask.count("loop", "3", "i"));
        wf.addStep(new ComputeTask("bad", "x", "1", ComputeTask.Operator.DIVIDE, "0"));
        wf.addStep(new EndLoopTask("end"));

        RunReport report = engine.run(wf);
        assertFalse(report.isSuccess());
    }

    @Test
    void jsonExtractFeedsAnIfDecision() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new SetVariableTask("payload", "body",
                "{\"status\":\"active\",\"count\":7}"));
        wf.addStep(new JsonExtractTask("extract", "${body}", "status", "status"));
        wf.addStep(new IfTask("if",
                new Condition("${status}", Condition.Comparator.EQUALS, "active")));
        wf.addStep(new SetVariableTask("on", "flag", "yes"));
        wf.addStep(new EndIfTask("end"));

        ExecutionContext ctx = new ExecutionContext();
        engine.run(wf, ctx, null);
        assertEquals("yes", ctx.get("flag"));
    }
}
