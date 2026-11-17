package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class ObjectNode extends NamedNode {
    public ObjectNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.OBJECT;
    }

    @Override
    protected String defaultName() {
        return ":Object";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitObject(this);
    }
}
