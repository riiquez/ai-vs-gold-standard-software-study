package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.PropertySpec;

public final class CallNode extends Node {
    public CallNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.CALL;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("openBottom", "false");
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(PropertySpec.bool("openBottom", "Open Bottom"));
    }

    @Override
    public boolean movable() {
        return false;
    }

    @Override
    public boolean followsPackageShift() {
        return false;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitCall(this);
    }
}
