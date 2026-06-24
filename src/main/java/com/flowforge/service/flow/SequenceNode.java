package com.flowforge.service.flow;

import com.flowforge.exception.WorkflowValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite node holding an ordered list of child nodes that run one after
 * another. A sequence aborts as soon as a child returns {@code false}, which
 * is how the fail-fast policy propagates up through nested blocks.
 */
public class SequenceNode extends FlowNode {

    private final List<FlowNode> children = new ArrayList<>();

    public void add(FlowNode child) {
        children.add(child);
    }

    public List<FlowNode> getChildren() {
        return children;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public boolean execute(FlowExecution execution) throws WorkflowValidationException {
        for (FlowNode child : children) {
            if (!child.execute(execution)) {
                return false; // a step failed: stop running this sequence
            }
        }
        return true;
    }
}
