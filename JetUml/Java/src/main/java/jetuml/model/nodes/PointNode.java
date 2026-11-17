package jetuml.model.nodes;

import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class PointNode extends Node {
    public PointNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.POINT;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of();
    }

    @Override
    public boolean followsPackageShift() {
        return false;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitPoint(this);
    }
}
