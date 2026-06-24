package com.flowforge.service.flow;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.IfTask;
import com.flowforge.model.task.LoopTask;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskType;

import java.util.List;

/**
 * Compiles a {@link Workflow}'s flat step list into a tree of {@link FlowNode}s
 * (the Composite structure the engine executes).
 * <p>
 * The flat list keeps persistence and the GUI simple - control flow is encoded
 * with marker steps (IF / ELSE / END IF, LOOP / END LOOP). This recursive
 * descent parser pairs those markers up into nested blocks, reporting a clear
 * {@link WorkflowValidationException} if they are unbalanced (e.g. an IF with
 * no END IF, or a stray ELSE). Each leaf remembers its original index so run
 * reports still line up with the steps the user sees.
 */
public final class FlowParser {

    private final List<Task> steps;
    private int position;

    private FlowParser(Workflow workflow) {
        this.steps = workflow.getSteps();
    }

    /** Compiles the workflow into an executable {@link SequenceNode}. */
    public static SequenceNode compile(Workflow workflow) throws WorkflowValidationException {
        FlowParser parser = new FlowParser(workflow);
        SequenceNode root = parser.parseSequence(null);
        if (parser.position != parser.steps.size()) {
            Task unexpected = parser.steps.get(parser.position);
            throw new WorkflowValidationException(
                    "Unexpected '" + unexpected.getType().getLabel() + "' step at position "
                            + (parser.position + 1) + " without a matching opening block.");
        }
        return root;
    }

    /**
     * Parses steps into a sequence until it reaches a terminator that belongs
     * to an enclosing block (ELSE/END_IF/END_LOOP) or the end of the list.
     *
     * @param terminators the marker types that should stop this sequence
     *                    (left for the caller to consume), or {@code null} at
     *                    the top level
     */
    private SequenceNode parseSequence(TaskType[] terminators) throws WorkflowValidationException {
        SequenceNode sequence = new SequenceNode();
        while (position < steps.size()) {
            Task task = steps.get(position);
            TaskType type = task.getType();

            if (isTerminator(type, terminators)) {
                return sequence;
            }

            switch (type) {
                case IF -> sequence.add(parseIf((IfTask) task));
                case LOOP -> sequence.add(parseLoop((LoopTask) task));
                case ELSE -> throw stray("Else", "If");
                case END_IF -> throw stray("End If", "If");
                case END_LOOP -> throw stray("End Loop", "Loop");
                default -> {
                    sequence.add(new TaskNode(task, position));
                    position++;
                }
            }
        }
        return sequence;
    }

    private IfNode parseIf(IfTask ifTask) throws WorkflowValidationException {
        int ifIndex = position;
        position++; // consume the IF marker

        SequenceNode thenBranch = parseSequence(new TaskType[]{TaskType.ELSE, TaskType.END_IF});
        SequenceNode elseBranch = new SequenceNode();

        if (atEnd()) {
            throw unclosed("If", ifIndex);
        }
        if (current().getType() == TaskType.ELSE) {
            position++; // consume ELSE
            elseBranch = parseSequence(new TaskType[]{TaskType.END_IF});
            if (atEnd()) {
                throw unclosed("If", ifIndex);
            }
        }
        // current() is now END_IF
        position++; // consume END_IF
        return new IfNode(ifTask, ifIndex, thenBranch, elseBranch);
    }

    private LoopNode parseLoop(LoopTask loopTask) throws WorkflowValidationException {
        int loopIndex = position;
        position++; // consume the LOOP marker

        SequenceNode body = parseSequence(new TaskType[]{TaskType.END_LOOP});
        if (atEnd()) {
            throw unclosed("Loop", loopIndex);
        }
        position++; // consume END_LOOP
        return new LoopNode(loopTask, loopIndex, body);
    }

    // ---------- helpers ----------

    private boolean isTerminator(TaskType type, TaskType[] terminators) {
        if (terminators == null) {
            return false;
        }
        for (TaskType t : terminators) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    private Task current() {
        return steps.get(position);
    }

    private boolean atEnd() {
        return position >= steps.size();
    }

    private WorkflowValidationException stray(String marker, String opener) {
        return new WorkflowValidationException(
                "'" + marker + "' at step " + (position + 1)
                        + " has no matching '" + opener + "'.");
    }

    private WorkflowValidationException unclosed(String block, int openIndex) {
        return new WorkflowValidationException(
                "'" + block + "' block opened at step " + (openIndex + 1) + " is never closed.");
    }
}
