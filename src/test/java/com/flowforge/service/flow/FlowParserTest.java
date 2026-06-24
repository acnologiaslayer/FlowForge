package com.flowforge.service.flow;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.Condition;
import com.flowforge.model.task.ElseTask;
import com.flowforge.model.task.EndIfTask;
import com.flowforge.model.task.EndLoopTask;
import com.flowforge.model.task.IfTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.LoopTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FlowParser}: the flat marker list is compiled into the
 * correct Composite tree and unbalanced blocks are rejected with clear errors.
 */
class FlowParserTest {

    private Workflow workflow() {
        return new Workflow("wf", "Flow", "");
    }

    private Condition cond() throws Exception {
        return new Condition("${x}", Condition.Comparator.GREATER, "5");
    }

    @Test
    void flatWorkflowBecomesSequenceOfTaskNodes() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new LogTask("a", "1"));
        wf.addStep(new LogTask("b", "2"));

        SequenceNode root = FlowParser.compile(wf);
        assertEquals(2, root.getChildren().size());
        assertInstanceOf(TaskNode.class, root.getChildren().get(0));
        assertEquals(0, ((TaskNode) root.getChildren().get(0)).getSourceIndex());
        assertEquals(1, ((TaskNode) root.getChildren().get(1)).getSourceIndex());
    }

    @Test
    void ifElseEndIfBecomesIfNode() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new IfTask("if", cond()));
        wf.addStep(new LogTask("then", "t"));
        wf.addStep(new ElseTask("else"));
        wf.addStep(new LogTask("else-body", "e"));
        wf.addStep(new EndIfTask("end"));

        SequenceNode root = FlowParser.compile(wf);
        assertEquals(1, root.getChildren().size());
        IfNode ifNode = assertInstanceOf(IfNode.class, root.getChildren().get(0));
        assertEquals(1, ifNode.getThenBranch().getChildren().size());
        assertEquals(1, ifNode.getElseBranch().getChildren().size());
    }

    @Test
    void ifWithoutElseHasEmptyElseBranch() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new IfTask("if", cond()));
        wf.addStep(new LogTask("then", "t"));
        wf.addStep(new EndIfTask("end"));

        IfNode ifNode = (IfNode) FlowParser.compile(wf).getChildren().get(0);
        assertEquals(1, ifNode.getThenBranch().getChildren().size());
        assertTrue(ifNode.getElseBranch().isEmpty());
    }

    @Test
    void loopBecomesLoopNodeWithBody() throws Exception {
        Workflow wf = workflow();
        wf.addStep(LoopTask.count("loop", "3", "i"));
        wf.addStep(new LogTask("body", "${i}"));
        wf.addStep(new EndLoopTask("end"));

        LoopNode loop = (LoopNode) FlowParser.compile(wf).getChildren().get(0);
        assertEquals(1, loop.getBody().getChildren().size());
    }

    @Test
    void nestedBlocksCompile() throws Exception {
        Workflow wf = workflow();
        wf.addStep(LoopTask.count("loop", "2", "i"));
        wf.addStep(new IfTask("if", cond()));
        wf.addStep(new LogTask("inner", "x"));
        wf.addStep(new EndIfTask("endif"));
        wf.addStep(new EndLoopTask("endloop"));

        LoopNode loop = (LoopNode) FlowParser.compile(wf).getChildren().get(0);
        assertInstanceOf(IfNode.class, loop.getBody().getChildren().get(0));
    }

    @Test
    void ifWithoutEndIfThrows() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new IfTask("if", cond()));
        wf.addStep(new LogTask("then", "t"));
        assertThrows(WorkflowValidationException.class, () -> FlowParser.compile(wf));
    }

    @Test
    void loopWithoutEndLoopThrows() throws Exception {
        Workflow wf = workflow();
        wf.addStep(LoopTask.count("loop", "3", "i"));
        wf.addStep(new LogTask("body", "x"));
        assertThrows(WorkflowValidationException.class, () -> FlowParser.compile(wf));
    }

    @Test
    void strayEndIfThrows() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new LogTask("a", "1"));
        wf.addStep(new EndIfTask("end"));
        assertThrows(WorkflowValidationException.class, () -> FlowParser.compile(wf));
    }

    @Test
    void strayElseThrows() throws Exception {
        Workflow wf = workflow();
        wf.addStep(new ElseTask("else"));
        assertThrows(WorkflowValidationException.class, () -> FlowParser.compile(wf));
    }

    @Test
    void crossedBlocksThrow() throws Exception {
        // LOOP ... IF ... END LOOP ... END IF  (improperly nested)
        Workflow wf = workflow();
        wf.addStep(LoopTask.count("loop", "2", "i"));
        wf.addStep(new IfTask("if", cond()));
        wf.addStep(new EndLoopTask("endloop"));
        wf.addStep(new EndIfTask("endif"));
        assertThrows(WorkflowValidationException.class, () -> FlowParser.compile(wf));
    }
}
