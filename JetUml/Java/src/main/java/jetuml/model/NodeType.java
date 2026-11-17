package jetuml.model;

public enum NodeType {
    CLASS("Class"),
    INTERFACE("Interface"),
    PACKAGE("Package"),
    PACKAGE_DESCRIPTION("PackageDescription"),
    NOTE("Note"),
    POINT("Point"),
    IMPLICIT_PARAMETER("ImplicitParameter"),
    CALL("Call"),
    STATE("State"),
    INITIAL_STATE("InitialState"),
    FINAL_STATE("FinalState"),
    OBJECT("Object"),
    FIELD("Field"),
    ACTOR("Actor"),
    USE_CASE("UseCase");

    private final String json;

    NodeType(String json) {
        this.json = json;
    }

    public String json() {
        return json;
    }

    public static NodeType fromJson(String value) {
        for (NodeType type : values()) {
            if (type.json.equals(value)) {
                return type;
            }
        }
        return null;
    }

    /** Tipos que pertencem a um único diagrama. Notas e pontos são universais. */
    public DiagramType home() {
        return switch (this) {
            case CLASS, INTERFACE, PACKAGE, PACKAGE_DESCRIPTION -> DiagramType.CLASS;
            case IMPLICIT_PARAMETER, CALL -> DiagramType.SEQUENCE;
            case STATE, INITIAL_STATE, FINAL_STATE -> DiagramType.STATE;
            case OBJECT, FIELD -> DiagramType.OBJECT;
            case ACTOR, USE_CASE -> DiagramType.USE_CASE;
            case NOTE, POINT -> null;
        };
    }
}
