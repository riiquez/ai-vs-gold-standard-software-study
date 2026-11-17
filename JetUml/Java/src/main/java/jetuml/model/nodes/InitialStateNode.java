package jetuml.model.nodes;

import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class InitialStateNode extends Node {
    public InitialStateNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.INITIAL_STATE;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInitialState(this);
    }
}
