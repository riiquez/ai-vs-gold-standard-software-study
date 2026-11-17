package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class ImplicitParameterNode extends NamedNode {
    public ImplicitParameterNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.IMPLICIT_PARAMETER;
    }

    @Override
    protected String defaultName() {
        return ":Object";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitImplicitParameter(this);
    }
}
