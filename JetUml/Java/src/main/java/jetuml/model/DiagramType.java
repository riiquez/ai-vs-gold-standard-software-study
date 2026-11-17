package jetuml.model;

public enum DiagramType {
    CLASS("Class", "Class diagram"),
    SEQUENCE("Sequence", "Sequence diagram"),
    STATE("State", "State diagram"),
    OBJECT("Object", "Object diagram"),
    USE_CASE("UseCase", "Use case diagram");

    private final String json;
    private final String title;

    DiagramType(String json, String title) {
        this.json = json;
        this.title = title;
    }

    public String json() {
        return json;
    }

    public String title() {
        return title;
    }

    public static DiagramType fromJson(String value) {
        for (DiagramType type : values()) {
            if (type.json.equals(value)) {
                return type;
            }
        }
        return null;
    }

    public boolean accepts(NodeType type) {
        if (type == NodeType.NOTE || type == NodeType.POINT) {
            return true;
        }
        return type.home() == this;
    }

    public boolean accepts(EdgeKind kind) {
        return switch (this) {
            case CLASS -> switch (kind) {
                case DEPENDENCY, INHERITANCE, REALIZATION, ASSOCIATION, AGGREGATION, COMPOSITION, NOTE -> true;
                default -> false;
            };
            case SEQUENCE -> kind == EdgeKind.CALL || kind == EdgeKind.RETURN || kind == EdgeKind.NOTE;
            case STATE -> kind == EdgeKind.TRANSITION || kind == EdgeKind.NOTE;
            case OBJECT -> kind == EdgeKind.REFERENCE || kind == EdgeKind.COLLABORATION || kind == EdgeKind.NOTE;
            case USE_CASE -> switch (kind) {
                case USE_ASSOCIATION, INCLUDE, EXTEND, GENERALIZATION, USE_DEPENDENCY, NOTE -> true;
                default -> false;
            };
        };
    }
}
