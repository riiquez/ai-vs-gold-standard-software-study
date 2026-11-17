package jetuml.model.nodes;

import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class FinalStateNode extends Node {
    public FinalStateNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.FINAL_STATE;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitFinalState(this);
    }
}
