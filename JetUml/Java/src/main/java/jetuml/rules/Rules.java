package jetuml.rules;

import jetuml.model.Diagram;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Node;
import jetuml.model.NodeType;

/** Regras estruturais de conexão (RF18, RF19 e os limites de cada diagrama). */
public final class Rules {
    private Rules() {
    }

    public static Check canCreate(Diagram diagram, EdgeKind kind, Node start, Node end) {
        if (start == null || end == null) {
            return Check.notify("Drop the relationship on a target element.");
        }
        if (kind == EdgeKind.NOTE) {
            return note(start, end);
        }
        if (diagram.type() == jetuml.model.DiagramType.SEQUENCE) {
            return Check.notify("Use the sequence tools to connect activation boxes.");
        }
        if (!diagram.type().accepts(kind)) {
            return Check.notify("This relationship does not belong to the current diagram.");
        }
        if (start == end) {
            Check selfCheck = self(diagram, kind, start);
            if (!selfCheck.ok()) {
                return selfCheck;
            }
            if (diagram.type() != jetuml.model.DiagramType.STATE && sameType(diagram, kind, start, end, null)) {
                return Check.notify("There is already an edge of this type between these elements.");
            }
            return Check.pass();
        }
        if (diagram.type() != jetuml.model.DiagramType.STATE && sameType(diagram, kind, start, end, null)) {
            return Check.notify("There is already an edge of this type between these elements.");
        }
        return switch (diagram.type()) {
            case CLASS -> classEdge(diagram, kind, start, end, null);
            case STATE -> stateEdge(diagram, kind, start, end, null);
            case OBJECT -> objectEdge(diagram, kind, start, end, null);
            case USE_CASE -> useCaseEdge(diagram, kind, start, end, null);
            case SEQUENCE -> Check.notify("Use the sequence tools to connect activation boxes.");
        };
    }

    public static Check canRetype(Diagram diagram, Edge edge, EdgeKind newKind) {
        if (edge.kind() == newKind) {
            return Check.pass();
        }
        if (edge.start() == edge.end()) {
            Check self = self(diagram, newKind, edge.start());
            if (!self.ok()) {
                return self;
            }
        } else if (diagram.type() != jetuml.model.DiagramType.STATE && sameType(diagram, newKind, edge.start(), edge.end(), edge)) {
            return Check.notify("There is already an edge of this type between these elements.");
        }
        return switch (diagram.type()) {
            case CLASS -> classEdge(diagram, newKind, edge.start(), edge.end(), edge);
            case STATE -> stateEdge(diagram, newKind, edge.start(), edge.end(), edge);
            case OBJECT -> objectEdge(diagram, newKind, edge.start(), edge.end(), edge);
            case USE_CASE -> useCaseEdge(diagram, newKind, edge.start(), edge.end(), edge);
            case SEQUENCE -> Check.pass();
        };
    }

    private static Check note(Node start, Node end) {
        if (start == end) {
            return Check.notify("A note cannot connect to itself.");
        }
        boolean involvesNote = start.type() == NodeType.NOTE || end.type() == NodeType.NOTE;
        boolean anchor = end.type() == NodeType.POINT || start.type() == NodeType.POINT;
        if (involvesNote || anchor) {
            return Check.pass();
        }
        return Check.notify("A note connector must be attached to a note.");
    }

    private static Check self(Diagram diagram, EdgeKind kind, Node node) {
        return switch (diagram.type()) {
            case CLASS -> switch (kind) {
                case INHERITANCE, DEPENDENCY, REALIZATION ->
                        Check.notify("Self-inheritance and self-dependency are not allowed.");
                case ASSOCIATION, AGGREGATION, COMPOSITION -> Check.pass();
                default -> Check.notify("This relationship cannot connect an element to itself.");
            };
            case USE_CASE -> Check.notify(
                    "Self-association, self-inheritance, and self-dependency are not allowed for actors and use cases.");
            case OBJECT -> kind == EdgeKind.COLLABORATION
                    ? Check.notify("Self-collaboration is not allowed.")
                    : Check.notify("This relationship cannot connect an element to itself.");
            case STATE -> stateEdge(diagram, kind, node, node, null);
            case SEQUENCE -> Check.notify("This relationship cannot connect an element to itself.");
        };
    }

    private static Check classEdge(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        if (!classifiable(start) || !classifiable(end)) {
            return Check.notify("This relationship connects classes, interfaces, or packages.");
        }
        if (kind.isClassCycleKind() && reverseCycle(diagram, start, end, ignore)) {
            return Check.notify("Direct cycles are not allowed for inheritance, dependency, aggregation, or association.");
        }
        if (mixesAssociationAndAggregation(diagram, kind, start, end, ignore)) {
            return Check.notify("Association and aggregation cannot be combined between the same classes.");
        }
        return Check.pass();
    }

    private static boolean classifiable(Node node) {
        return node.type() == NodeType.CLASS || node.type() == NodeType.INTERFACE || node.type() == NodeType.PACKAGE;
    }

    private static Check stateEdge(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        if (kind != EdgeKind.TRANSITION) {
            return Check.notify("States are connected by transitions.");
        }
        if (!isState(start) || !isState(end)) {
            return Check.notify("Transitions connect states.");
        }
        if (start.type() == NodeType.FINAL_STATE) {
            return Check.notify("Transitions cannot leave a final state.");
        }
        if (end.type() == NodeType.INITIAL_STATE) {
            return Check.notify("Transitions cannot arrive at an initial state.");
        }
        int count = 0;
        for (Edge edge : diagram.edges()) {
            if (edge == ignore || edge.kind() != EdgeKind.TRANSITION) {
                continue;
            }
            if (edge.start() == start && edge.end() == end) {
                count++;
            }
        }
        if (count >= 2) {
            return start == end
                    ? Check.notify("A state accepts at most two self-transitions.")
                    : Check.notify("At most two transitions are allowed in each direction.");
        }
        return Check.pass();
    }

    private static boolean isState(Node node) {
        return node.type() == NodeType.STATE || node.type() == NodeType.INITIAL_STATE || node.type() == NodeType.FINAL_STATE;
    }

    private static Check objectEdge(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        if (kind == EdgeKind.REFERENCE) {
            if (start.type() != NodeType.FIELD || end.type() != NodeType.OBJECT) {
                return Check.notify("A reference starts at a field value and ends at an object.");
            }
            return Check.pass();
        }
        if (kind == EdgeKind.COLLABORATION) {
            if (start.type() != NodeType.OBJECT || end.type() != NodeType.OBJECT) {
                return Check.notify("A collaboration connects two objects.");
            }
            if (collaborationBetween(diagram, start, end, ignore)) {
                return Check.notify("Collaboration cycles between two objects are not allowed.");
            }
            return Check.pass();
        }
        return Check.notify("Objects use references and collaborations.");
    }

    private static Check useCaseEdge(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        boolean actorStart = start.type() == NodeType.ACTOR;
        boolean actorEnd = end.type() == NodeType.ACTOR;
        boolean useStart = start.type() == NodeType.USE_CASE;
        boolean useEnd = end.type() == NodeType.USE_CASE;
        if (!(actorStart || useStart) || !(actorEnd || useEnd)) {
            return Check.notify("This relationship connects actors and use cases.");
        }
        return switch (kind) {
            case INCLUDE, EXTEND, USE_DEPENDENCY -> (useStart && useEnd)
                    ? Check.pass()
                    : Check.notify("Include and extend connect two use cases.");
            case GENERALIZATION -> ((actorStart && actorEnd) || (useStart && useEnd))
                    ? Check.pass()
                    : Check.notify("Generalization connects two actors or two use cases.");
            case USE_ASSOCIATION -> Check.pass();
            default -> Check.notify("This relationship does not belong to a use case diagram.");
        };
    }

    private static boolean sameType(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        EdgeKind family = family(kind);
        for (Edge edge : diagram.edges()) {
            if (edge == ignore) {
                continue;
            }
            if (family(edge.kind()) != family) {
                continue;
            }
            boolean same = (edge.start() == start && edge.end() == end) || (edge.start() == end && edge.end() == start);
            if (same) {
                return true;
            }
        }
        return false;
    }

    /** Agregação e composição são a mesma aresta com Aggregation Type. */
    private static EdgeKind family(EdgeKind kind) {
        if (kind.isAggregationFamily()) {
            return EdgeKind.AGGREGATION;
        }
        return kind;
    }

    private static boolean reverseCycle(Diagram diagram, Node start, Node end, Edge ignore) {
        for (Edge edge : diagram.edges()) {
            if (edge == ignore || !edge.kind().isClassCycleKind()) {
                continue;
            }
            if (edge.start() == end && edge.end() == start) {
                return true;
            }
        }
        return false;
    }

    private static boolean mixesAssociationAndAggregation(Diagram diagram, EdgeKind kind, Node start, Node end, Edge ignore) {
        boolean addingAssociation = kind == EdgeKind.ASSOCIATION;
        boolean addingAggregation = kind.isAggregationFamily();
        if (!addingAssociation && !addingAggregation) {
            return false;
        }
        for (Edge edge : diagram.edges()) {
            if (edge == ignore) {
                continue;
            }
            boolean pair = (edge.start() == start && edge.end() == end) || (edge.start() == end && edge.end() == start);
            if (!pair) {
                continue;
            }
            if (addingAssociation && edge.kind().isAggregationFamily()) {
                return true;
            }
            if (addingAggregation && edge.kind() == EdgeKind.ASSOCIATION) {
                return true;
            }
        }
        return false;
    }

    private static boolean collaborationBetween(Diagram diagram, Node start, Node end, Edge ignore) {
        for (Edge edge : diagram.edges()) {
            if (edge == ignore || edge.kind() != EdgeKind.COLLABORATION) {
                continue;
            }
            boolean pair = (edge.start() == start && edge.end() == end) || (edge.start() == end && edge.end() == start);
            if (pair) {
                return true;
            }
        }
        return false;
    }
}
