package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.PropertySpec;

public final class ClassNode extends Node {
    public ClassNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.CLASS;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("name", "Class", "attributes", "", "methods", "");
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(
                PropertySpec.text("name", "Name"),
                PropertySpec.multi("attributes", "Attributes"),
                PropertySpec.multi("methods", "Methods"));
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitClass(this);
    }
}
