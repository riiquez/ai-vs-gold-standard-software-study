package jetuml.model.nodes;

import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;

public final class PackageNode extends NamedNode {
    public PackageNode(String id, int x, int y) {
        super(id, x, y);
    }

    @Override
    public NodeType type() {
        return NodeType.PACKAGE;
    }

    @Override
    protected String defaultName() {
        return "Package";
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitPackage(this);
    }
}
