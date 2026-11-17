package jetuml.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jetuml.model.nodes.ActorNode;
import jetuml.model.nodes.CallNode;
import jetuml.model.nodes.ClassNode;
import jetuml.model.nodes.FieldNode;
import jetuml.model.nodes.FinalStateNode;
import jetuml.model.nodes.ImplicitParameterNode;
import jetuml.model.nodes.InitialStateNode;
import jetuml.model.nodes.InterfaceNode;
import jetuml.model.nodes.NoteNode;
import jetuml.model.nodes.ObjectNode;
import jetuml.model.nodes.PackageDescriptionNode;
import jetuml.model.nodes.PackageNode;
import jetuml.model.nodes.PointNode;
import jetuml.model.nodes.StateNode;
import jetuml.model.nodes.UseCaseNode;

/**
 * Nó do diagrama. Cada figura UML é uma subclasse; a posição e as propriedades
 * ficam nesta base e são compostas pelo {@link Diagram}.
 */
public abstract class Node implements Element {
    private final String id;
    private int x;
    private int y;
    private Node parent;
    private final Map<String, String> props = new LinkedHashMap<>();

    protected Node(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        props.putAll(defaultProperties());
    }

    public static Node create(String id, NodeType type, int x, int y) {
        return switch (type) {
            case CLASS -> new ClassNode(id, x, y);
            case INTERFACE -> new InterfaceNode(id, x, y);
            case PACKAGE -> new PackageNode(id, x, y);
            case PACKAGE_DESCRIPTION -> new PackageDescriptionNode(id, x, y);
            case NOTE -> new NoteNode(id, x, y);
            case POINT -> new PointNode(id, x, y);
            case IMPLICIT_PARAMETER -> new ImplicitParameterNode(id, x, y);
            case CALL -> new CallNode(id, x, y);
            case STATE -> new StateNode(id, x, y);
            case INITIAL_STATE -> new InitialStateNode(id, x, y);
            case FINAL_STATE -> new FinalStateNode(id, x, y);
            case OBJECT -> new ObjectNode(id, x, y);
            case FIELD -> new FieldNode(id, x, y);
            case ACTOR -> new ActorNode(id, x, y);
            case USE_CASE -> new UseCaseNode(id, x, y);
        };
    }

    public abstract NodeType type();

    public abstract <T> T accept(NodeVisitor<T> visitor);

    protected abstract Map<String, String> defaultProperties();

    @Override
    public String id() {
        return id;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public Node parent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public boolean movable() {
        return true;
    }

    /** Shift prende ou solta o nó de um pacote. Campo, ativação e ponto ficam de fora. */
    public boolean followsPackageShift() {
        return true;
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of();
    }

    @Override
    public String get(String key) {
        return props.getOrDefault(key, "");
    }

    @Override
    public void set(String key, String value) {
        props.put(key, value == null ? "" : value);
    }

    public boolean flag(String key) {
        return "true".equals(get(key));
    }

    public Map<String, String> properties() {
        return props;
    }

    public Node copy(String newId) {
        Node copy = create(newId, type(), x, y);
        copy.props.clear();
        copy.props.putAll(props);
        return copy;
    }
}
