package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class UseCaseNode extends NamedNode {
    public UseCaseNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.USE_CASE;
    }

    @Override
    protected String defaultName() {
        return "Use Case";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitUseCase(this);
    }
}