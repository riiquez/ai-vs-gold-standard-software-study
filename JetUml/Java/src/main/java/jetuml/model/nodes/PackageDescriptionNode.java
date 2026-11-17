package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.PropertySpec;

public final class PackageDescriptionNode extends Node {
    public PackageDescriptionNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.PACKAGE_DESCRIPTION;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("name", "Description", "content", "");
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(
                PropertySpec.text("name", "Name"),
                PropertySpec.multi("content", "Content"));
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitPackageDescription(this);
    }
}
