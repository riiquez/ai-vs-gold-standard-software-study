package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class StateNode extends NamedNode {
    public StateNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.STATE;
    }

    @Override
    protected String defaultName() {
        return "State";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitState(this);
    }
}
