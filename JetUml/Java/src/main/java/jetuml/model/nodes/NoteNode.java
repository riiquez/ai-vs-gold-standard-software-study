package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.PropertySpec;

public final class NoteNode extends Node {
    public NoteNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.NOTE;
    }

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("text", "Note");
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(PropertySpec.multi("text", "Text"));
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitNote(this);
    }
}
