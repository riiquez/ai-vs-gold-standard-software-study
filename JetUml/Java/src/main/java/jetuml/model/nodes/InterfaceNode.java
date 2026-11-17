package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.PropertySpec;

public final class InterfaceNode extends Node {
    public InterfaceNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.INTERFACE;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("name", "Interface", "methods", "");
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(
                PropertySpec.text("name", "Name"),
                PropertySpec.multi("methods", "Methods"));
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInterface(this);
    }
}
