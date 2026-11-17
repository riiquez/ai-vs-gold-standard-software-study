package jetuml.app;

import jetuml.model.DiagramType;
import jetuml.model.EdgeKind;
import jetuml.model.NodeType;

/** Ferramenta da barra lateral. A ordem em cada diagrama é definida por DiagramType. */
public enum Tool {
    SELECT("Select", "Select elements, move them, or drag on empty space to select a region."),
    CLASS("Class", "A UML class with a name, attributes, and methods. Abstract names use /slashes/ and static members use _underscores_."),
    INTERFACE("Interface", "A UML interface with a name and methods."),
    PACKAGE("Package", "A package that can contain other nodes. Click inside it to create a child. Press Shift to attach or detach a node."),
    PACKAGE_DESCRIPTION("Package Description", "A package annotation with a name and free content."),
    NOTE("Note", "A note with free text, available in every diagram."),
    DEPENDENCY("Dependency", "A dashed dependency. Generalization Type and Directionality are edited on the edge."),
    INHERITANCE("Inheritance", "A generalization. It can be switched to implementation on the edge itself."),
    REALIZATION("Realization", "An implementation relationship, drawn as a dashed line with a hollow triangle."),
    ASSOCIATION("Association", "An association with start, middle, and end labels and a directionality."),
    AGGREGATION("Aggregation", "An aggregation. Aggregation Type switches it to composition without recreating the edge."),
    COMPOSITION("Composition", "A composition. Aggregation Type switches it to aggregation without recreating the edge."),
    NOTE_CONNECTOR("Note Connector", "Connect a node to a note. Drag from a note to an empty point to anchor that end."),
    TO_CLIPBOARD("To Clipboard", "Copy an image of the whole diagram, without the grid and cropped to the content."),
    IMPLICIT_PARAMETER("Implicit Parameter", "A sequence participant: a named box with a lifeline."),
    CALL("Call", "Drag from a lifeline or an activation box to create a call. Drag inside an activation to create a self-call."),
    RETURN("Return", "A return message between activation boxes."),
    STATE("State", "A state with a name."),
    INITIAL_STATE("Initial State", "The initial pseudostate. It has no text properties, and transitions cannot enter it."),
    FINAL_STATE("Final State", "The final state. It has no text properties, and transitions cannot leave it."),
    TRANSITION("Transition", "A transition with a middle label. Up to two transitions are allowed in each direction, including self-transitions."),
    OBJECT("Object", "An object instance. Its name is underlined."),
    FIELD("Field", "A field inside an object. Name and value are edited in the properties. A field cannot exist outside an object."),
    REFERENCE("Reference", "Drag from the value side of a field to an object."),
    COLLABORATION("Collaboration", "A collaboration between two objects, with a middle label. Cycles and self-links are rejected."),
    ACTOR("Actor", "An actor in a use case diagram."),
    USE_CASE("Use Case", "A use case, drawn as an ellipse."),
    UC_ASSOCIATION("Association", "An association between an actor and a use case, or between use cases."),
    INCLUDE("Include", "An \u00ABinclude\u00BB dependency between use cases. Dependency Type can switch it to extend or none."),
    EXTEND("Extend", "An \u00ABextend\u00BB dependency between use cases. Dependency Type can switch it to include or none."),
    GENERALIZATION("Generalization", "A generalization between two actors or two use cases.");

    private final String label;
    private final String verbose;

    Tool(String label, String verbose) {
        this.label = label;
        this.verbose = verbose;
    }

    public String label() {
        return label;
    }

    public String verbose() {
        return verbose;
    }

    public boolean isAction() {
        return this == TO_CLIPBOARD;
    }

    public NodeType nodeType() {
        return switch (this) {
            case CLASS -> NodeType.CLASS;
            case INTERFACE -> NodeType.INTERFACE;
            case PACKAGE -> NodeType.PACKAGE;
            case PACKAGE_DESCRIPTION -> NodeType.PACKAGE_DESCRIPTION;
            case NOTE -> NodeType.NOTE;
            case IMPLICIT_PARAMETER -> NodeType.IMPLICIT_PARAMETER;
            case STATE -> NodeType.STATE;
            case INITIAL_STATE -> NodeType.INITIAL_STATE;
            case FINAL_STATE -> NodeType.FINAL_STATE;
            case OBJECT -> NodeType.OBJECT;
            case FIELD -> NodeType.FIELD;
            case ACTOR -> NodeType.ACTOR;
            case USE_CASE -> NodeType.USE_CASE;
            default -> null;
        };
    }

    public EdgeKind edgeKind() {
        return switch (this) {
            case DEPENDENCY -> EdgeKind.DEPENDENCY;
            case INHERITANCE -> EdgeKind.INHERITANCE;
            case REALIZATION -> EdgeKind.REALIZATION;
            case ASSOCIATION -> EdgeKind.ASSOCIATION;
            case AGGREGATION -> EdgeKind.AGGREGATION;
            case COMPOSITION -> EdgeKind.COMPOSITION;
            case NOTE_CONNECTOR -> EdgeKind.NOTE;
            case CALL -> EdgeKind.CALL;
            case RETURN -> EdgeKind.RETURN;
            case TRANSITION -> EdgeKind.TRANSITION;
            case REFERENCE -> EdgeKind.REFERENCE;
            case COLLABORATION -> EdgeKind.COLLABORATION;
            case UC_ASSOCIATION -> EdgeKind.USE_ASSOCIATION;
            case INCLUDE -> EdgeKind.INCLUDE;
            case EXTEND -> EdgeKind.EXTEND;
            case GENERALIZATION -> EdgeKind.GENERALIZATION;
            default -> null;
        };
    }

    /** Ordem da barra, dos atalhos e do menu de contexto (RF07, RF08, RF09). */
    public static Tool[] forDiagram(DiagramType type) {
        return switch (type) {
            case CLASS -> new Tool[] {
                    SELECT, CLASS, INTERFACE, PACKAGE, PACKAGE_DESCRIPTION, NOTE,
                    DEPENDENCY, INHERITANCE, REALIZATION, ASSOCIATION, AGGREGATION,
                    COMPOSITION, NOTE_CONNECTOR, TO_CLIPBOARD
            };
            case SEQUENCE -> new Tool[] {
                    SELECT, IMPLICIT_PARAMETER, NOTE, CALL, RETURN, NOTE_CONNECTOR, TO_CLIPBOARD
            };
            case STATE -> new Tool[] {
                    SELECT, STATE, INITIAL_STATE, FINAL_STATE, NOTE, TRANSITION, NOTE_CONNECTOR, TO_CLIPBOARD
            };
            case OBJECT -> new Tool[] {
                    SELECT, OBJECT, FIELD, NOTE, REFERENCE, COLLABORATION, NOTE_CONNECTOR, TO_CLIPBOARD
            };
            case USE_CASE -> new Tool[] {
                    SELECT, ACTOR, USE_CASE, NOTE, UC_ASSOCIATION, INCLUDE, EXTEND,
                    GENERALIZATION, NOTE_CONNECTOR, TO_CLIPBOARD
            };
        };
    }

    /** Rótulo do atalho 1–9, 0, A, B… conforme a posição na barra. */
    public static String shortcut(int index) {
        if (index < 9) {
            return Integer.toString(index + 1);
        }
        if (index == 9) {
            return "0";
        }
        return String.valueOf((char) ('A' + (index - 10)));
    }
}
