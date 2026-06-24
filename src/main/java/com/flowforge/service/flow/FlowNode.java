package com.flowforge.service.flow;

import com.flowforge.exception.WorkflowValidationException;

/**
 * A node in a compiled workflow tree.
 * <p>
 * This is the <b>component</b> of the Composite pattern: a {@link TaskNode}
 * is a leaf (a single step), while {@link SequenceNode}, {@link IfNode} and
 * {@link LoopNode} are composites that contain child nodes. The engine builds
 * this tree from the flat step list (see {@link FlowParser}) and then walks it
 * recursively, which is what turns a plain list of steps into real branching
 * and looping control flow.
 */
public abstract class FlowNode {

    /**
     * Runs this node against the given execution.
     *
     * @return {@code true} to continue the workflow, {@code false} to abort
     *         (a step failed, so the fail-fast policy stops the run)
     */
    public abstract boolean execute(FlowExecution execution) throws WorkflowValidationException;
}
