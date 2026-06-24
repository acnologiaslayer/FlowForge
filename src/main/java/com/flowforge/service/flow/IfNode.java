package com.flowforge.service.flow;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.task.IfTask;

/**
 * Composite node implementing {@code IF ... ELSE ... END IF}.
 * <p>
 * It first runs the IF marker step (so the decision is timed and recorded in
 * the report), then evaluates the {@link com.flowforge.model.task.Condition}
 * and executes either the "then" or the "else" branch - never both. Either
 * branch may itself contain further IF/LOOP blocks, since branches are plain
 * {@link SequenceNode}s of {@link FlowNode}s.
 */
public class IfNode extends FlowNode {

    private final IfTask task;
    private final int sourceIndex;
    private final SequenceNode thenBranch;
    private final SequenceNode elseBranch;

    public IfNode(IfTask task, int sourceIndex,
                  SequenceNode thenBranch, SequenceNode elseBranch) {
        this.task = task;
        this.sourceIndex = sourceIndex;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public SequenceNode getThenBranch() {
        return thenBranch;
    }

    public SequenceNode getElseBranch() {
        return elseBranch;
    }

    @Override
    public boolean execute(FlowExecution execution) throws WorkflowValidationException {
        if (!execution.runTask(task, sourceIndex)) {
            return false;
        }
        boolean condition = task.getCondition().evaluate(execution.getContext());
        return condition
                ? thenBranch.execute(execution)
                : elseBranch.execute(execution);
    }
}
