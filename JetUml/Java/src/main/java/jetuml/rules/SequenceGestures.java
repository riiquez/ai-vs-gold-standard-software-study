package jetuml.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jetuml.cmd.Command;
import jetuml.cmd.Commands;
import jetuml.model.Diagram;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Geom;
import jetuml.model.Node;
import jetuml.model.NodeType;

/**
 * Criação de chamadas, autochamadas, construtores e retornos.
 * Saltos no fluxo falham em silêncio; infrações estruturais geram erro (RF27–RF31).
 */
public final class SequenceGestures {
    public static final int MIN_HEIGHT = 36;

    private SequenceGestures() {
    }

    public record Result(Check check, Command command, Node focus) {
        static Result of(Check check) {
            return new Result(check, null, null);
        }

        static Result ok(Command command, Node focus) {
            return new Result(Check.pass(), command, focus);
        }
    }

    public static Result create(Diagram diagram, Node source, boolean sourceHeader, Node target, boolean targetHeader, int y) {
        if (source == null) {
            return Result.of(Check.notify("Start the call on a lifeline or an activation box."));
        }
        y = Geom.snap(y);
        if (source.type() == NodeType.CALL && target == source) {
            return selfCall(diagram, source, y);
        }
        if (target == null) {
            return Result.of(Check.silent());
        }
        if (targetHeader && target.type() == NodeType.IMPLICIT_PARAMETER) {
            return constructor(diagram, source, sourceHeader, target, y);
        }
        if (target.type() == NodeType.CALL) {
            if (source.type() == NodeType.CALL && sameLife(source, target) && containsY(diagram, source, y)) {
                return selfCall(diagram, source, y);
            }
            return Result.of(Check.notify("An activation box cannot receive calls from multiple callers."));
        }
        if (target.type() != NodeType.IMPLICIT_PARAMETER) {
            return Result.of(Check.notify("Call and return edges can only connect activation boxes."));
        }
        if (source.type() == NodeType.CALL) {
            return callFromActivation(diagram, source, target, y);
        }
        if (source.type() == NodeType.IMPLICIT_PARAMETER && !sourceHeader) {
            return foundMessage(diagram, source, target, y);
        }
        return Result.of(Check.notify("Call and return edges can only connect activation boxes."));
    }

    public static Result createReturn(Diagram diagram, Node source, Node target) {
        if (source == null || target == null) {
            return Result.of(Check.silent());
        }
        if (source.type() != NodeType.CALL || target.type() != NodeType.CALL) {
            return Result.of(Check.notify("Call and return edges can only connect activation boxes."));
        }
        Edge match = null;
        for (Edge edge : diagram.edges()) {
            if (edge.kind() == EdgeKind.CALL && edge.start() == target && edge.end() == source) {
                match = edge;
                break;
            }
        }
        if (match == null) {
            return Result.of(Check.silent());
        }
        for (Edge edge : diagram.edges()) {
            if (edge.kind() == EdgeKind.RETURN && edge.start() == source && edge.end() == target) {
                return Result.of(Check.notify("There is already a return between these activation boxes."));
            }
        }
        Edge back = new Edge(diagram.nextEdgeId(), EdgeKind.RETURN, source, target);
        return Result.ok(Commands.addEdge(diagram, back), source);
    }

    private static Result foundMessage(Diagram diagram, Node source, Node target, int y) {
        if (diagram.rootActivations() >= 1) {
            return Result.of(Check.notify("Only one root activation is allowed in a sequence diagram."));
        }
        if (source == target) {
            return Result.of(Check.silent());
        }
        if (!belowHeader(source, y) || !belowHeader(target, y)) {
            return Result.of(Check.silent());
        }
        if (!fits(diagram, source, y, null) || !fits(diagram, target, y, null)) {
            return Result.of(Check.silent());
        }
        Node caller = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, y);
        caller.setParent(source);
        Node callee = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, y);
        callee.setParent(target);
        Edge edge = new Edge(diagram.nextEdgeId(), EdgeKind.CALL, caller, callee);
        return Result.ok(Commands.compound(List.of(
                Commands.addNode(diagram, caller),
                Commands.addNode(diagram, callee),
                Commands.addEdge(diagram, edge))), callee);
    }

    private static Result callFromActivation(Diagram diagram, Node source, Node target, int y) {
        if (!isAppendPosition(diagram, source, y)) {
            return Result.of(Check.silent());
        }
        if (target == source.parent()) {
            return selfCall(diagram, source, y);
        }
        if (!fits(diagram, target, y, null)) {
            return Result.of(Check.silent());
        }
        Node callee = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, y);
        callee.setParent(target);
        Edge edge = new Edge(diagram.nextEdgeId(), EdgeKind.CALL, source, callee);
        return Result.ok(Commands.compound(List.of(
                Commands.addNode(diagram, callee),
                Commands.addEdge(diagram, edge))), callee);
    }

    private static Result selfCall(Diagram diagram, Node source, int y) {
        if (!isAppendPosition(diagram, source, y)) {
            return Result.of(Check.silent());
        }
        int nestedY = Math.max(y, source.y() + 16);
        if (!fits(diagram, source.parent(), nestedY, source)) {
            return Result.of(Check.silent());
        }
        Node callee = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, nestedY);
        callee.setParent(source.parent());
        Edge edge = new Edge(diagram.nextEdgeId(), EdgeKind.CALL, source, callee);
        return Result.ok(Commands.compound(List.of(
                Commands.addNode(diagram, callee),
                Commands.addEdge(diagram, edge))), callee);
    }

    private static Result constructor(Diagram diagram, Node source, boolean sourceHeader, Node target, int y) {
        if (source == target) {
            return Result.of(Check.notify("A constructor call must target another participant."));
        }
        Node callerActivation;
        List<Command> parts = new ArrayList<>();
        if (source.type() == NodeType.CALL) {
            if (!isAppendPosition(diagram, source, y)) {
                return Result.of(Check.silent());
            }
            callerActivation = source;
        } else if (source.type() == NodeType.IMPLICIT_PARAMETER && !sourceHeader) {
            if (diagram.rootActivations() >= 1) {
                return Result.of(Check.notify("Only one root activation is allowed in a sequence diagram."));
            }
            if (!belowHeader(source, y) || !fits(diagram, source, y, null)) {
                return Result.of(Check.silent());
            }
            callerActivation = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, y);
            callerActivation.setParent(source);
            parts.add(Commands.addNode(diagram, callerActivation));
        } else {
            return Result.of(Check.notify("Call and return edges can only connect activation boxes."));
        }
        int delta = y - headerCenter(target);
        int calleeY = target.y() + delta + headerHeight(target);
        int extra = 0;
        for (Node call : callsOn(diagram, target)) {
            int shifted = call.y() + delta;
            if (shifted < calleeY + MIN_HEIGHT + 8) {
                extra = Math.max(extra, calleeY + MIN_HEIGHT + 8 - shifted);
            }
        }
        List<Node> moved = new ArrayList<>();
        List<Integer> oldY = new ArrayList<>();
        List<Integer> newY = new ArrayList<>();
        moved.add(target);
        oldY.add(target.y());
        newY.add(target.y() + delta);
        for (Node call : callsOn(diagram, target)) {
            moved.add(call);
            oldY.add(call.y());
            newY.add(call.y() + delta + extra);
        }
        Node callee = Node.create(diagram.nextNodeId(), NodeType.CALL, 0, calleeY);
        callee.setParent(target);
        Edge edge = new Edge(diagram.nextEdgeId(), EdgeKind.CALL, callerActivation, callee);
        edge.setConstructor(true);
        int[] fromX = new int[moved.size()];
        int[] fromY = new int[moved.size()];
        int[] toX = new int[moved.size()];
        int[] toY = new int[moved.size()];
        for (int i = 0; i < moved.size(); i++) {
            fromX[i] = moved.get(i).x();
            toX[i] = fromX[i];
            fromY[i] = oldY.get(i);
            toY[i] = newY.get(i);
        }
        parts.add(new Commands.Move(diagram, moved, fromX, fromY, toX, toY));
        parts.add(Commands.addNode(diagram, callee));
        parts.add(Commands.addEdge(diagram, edge));
        return Result.ok(Commands.compound(parts), callee);
    }

    public static int headerHeight(Node life) {
        return 44;
    }

    public static int headerCenter(Node life) {
        return life.y() + headerHeight(life) / 2;
    }

    public static boolean belowHeader(Node life, int y) {
        return y >= life.y() + headerHeight(life);
    }

    public static List<Node> callsOn(Diagram diagram, Node life) {
        List<Node> calls = new ArrayList<>();
        for (Node child : diagram.children(life)) {
            if (child.type() == NodeType.CALL) {
                calls.add(child);
            }
        }
        calls.sort((a, b) -> Integer.compare(a.y(), b.y()));
        return calls;
    }

    public static int height(Diagram diagram, Node call) {
        return height(diagram, call, new HashSet<>());
    }

    private static int height(Diagram diagram, Node call, Set<Node> stack) {
        if (!stack.add(call)) {
            return MIN_HEIGHT;
        }
        int bottom = call.y() + MIN_HEIGHT;
        for (Edge edge : diagram.edges()) {
            if (edge.kind() == EdgeKind.CALL && edge.start() == call && edge.end() != null && edge.end().type() == NodeType.CALL) {
                bottom = Math.max(bottom, edge.end().y() + height(diagram, edge.end(), stack) + 16);
            }
        }
        stack.remove(call);
        if (call.flag("openBottom")) {
            bottom += 24;
        }
        return Math.max(MIN_HEIGHT, bottom - call.y());
    }

    public static int nestLevel(Diagram diagram, Node call) {
        Node caller = diagram.callerOf(call);
        if (caller != null && caller.type() == NodeType.CALL && caller.parent() == call.parent()) {
            return nestLevel(diagram, caller) + 1;
        }
        return 0;
    }

    private static boolean sameLife(Node a, Node b) {
        return a.parent() != null && a.parent() == b.parent();
    }

    private static boolean containsY(Diagram diagram, Node call, int y) {
        return y >= call.y() && y <= call.y() + height(diagram, call);
    }

    /** Só o fim do bloco corrente é um ponto válido. Inserir no meio seria um salto. */
    public static boolean isAppendPosition(Diagram diagram, Node caller, int y) {
        int start = caller.y() + 14;
        int last = start;
        for (Edge edge : diagram.edges()) {
            if (edge.kind() == EdgeKind.CALL && edge.start() == caller && edge.end() != null) {
                last = Math.max(last, edge.end().y() + height(diagram, edge.end()) + 8);
            }
        }
        int end = caller.y() + height(diagram, caller);
        return y >= last - 6 && y <= end + 8;
    }

    private static boolean fits(Diagram diagram, Node life, int y, Node parentCall) {
        if (life == null || !belowHeader(life, y)) {
            return false;
        }
        int bottom = y + MIN_HEIGHT;
        if (parentCall != null) {
            int parentBottom = parentCall.y() + height(diagram, parentCall);
            if (y < parentCall.y() + 8 || y > parentBottom + 4) {
                return false;
            }
        }
        for (Node other : callsOn(diagram, life)) {
            if (other == parentCall) {
                continue;
            }
            int otherBottom = other.y() + height(diagram, other);
            boolean overlap = y < otherBottom && other.y() < bottom;
            if (!overlap) {
                continue;
            }
            if (parentCall != null && (other == parentCall || isNestedIn(diagram, parentCall, other))) {
                continue;
            }
            if (parentCall != null && isNestedIn(diagram, other, parentCall)) {
                return false;
            }
            return false;
        }
        return true;
    }

    private static boolean isNestedIn(Diagram diagram, Node node, Node ancestor) {
        Node current = node;
        while (current != null) {
            Node caller = diagram.callerOf(current);
            if (caller == ancestor) {
                return true;
            }
            current = caller != null && caller.type() == NodeType.CALL ? caller : null;
        }
        return false;
    }
}
