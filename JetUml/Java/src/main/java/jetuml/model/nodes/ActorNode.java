package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class ActorNode extends NamedNode {
    public ActorNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.ACTOR;
    }

    @Override
    protected String defaultName() {
        return "Actor";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitActor(this);
    }
}
