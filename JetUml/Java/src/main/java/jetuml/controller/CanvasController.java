package jetuml.controller;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jetuml.app.Tool;
import jetuml.cmd.Command;
import jetuml.cmd.Commands;
import jetuml.model.Diagram;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Element;
import jetuml.model.Geom;
import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.rules.Check;
import jetuml.rules.Rules;
import jetuml.rules.SequenceGestures;
import jetuml.view.Layout;

/** Controle do mouse: seleção, criação de nós e arestas. A visão só traduz o ponteiro. */
public final class CanvasController {
    private enum Mode { NONE, MOVE, RUBBER, EDGE }

    private final CanvasHost host;
    private final Set<Element> selected = new LinkedHashSet<>();
    private Mode mode = Mode.NONE;
    private int pressX;
    private int pressY;
    private int pressScreenX;
    private int pressScreenY;
    private boolean dragged;
    private Element clickElement;
    private boolean clickWasSelected;
    private Node edgeSource;
    private boolean edgeSourceHeader;
    private int currentX;
    private int currentY;
    private final Map<Node, Point> origins = new LinkedHashMap<>();
    private int pendingX;
    private int pendingY;
    private boolean pendingCreate;

    public CanvasController(CanvasHost host) {
        this.host = host;
    }

    public Set<Element> selection() {
        return selected;
    }

    public void refreshSelection() {
        Diagram diagram = host.diagram();
        selected.removeIf(element -> element instanceof Node node
                ? !diagram.nodes().contains(node)
                : element instanceof Edge edge && !diagram.edges().contains(edge));
    }

    public void selectAll() {
        selected.clear();
        selected.addAll(host.diagram().nodes());
        selected.addAll(host.diagram().edges());
        host.repaintCanvas();
    }

    public void clearSelection() {
        selected.clear();
        host.repaintCanvas();
    }

    public Rectangle rubber() {
        if (mode != Mode.RUBBER || !dragged) {
            return null;
        }
        int x = Math.min(pressX, currentX);
        int y = Math.min(pressY, currentY);
        return new Rectangle(x, y, Math.abs(currentX - pressX), Math.abs(currentY - pressY));
    }

    public int[] band() {
        if (mode != Mode.EDGE || edgeSource == null) {
            return null;
        }
        Point anchor = anchor(edgeSource);
        return new int[] {anchor.x, anchor.y, currentX, currentY};
    }

    public boolean hasPendingCreate() {
        return pendingCreate;
    }

    public void cancelPendingCreate() {
        pendingCreate = false;
    }

    public void press(int modelX, int modelY, int screenX, int screenY, boolean control, int clickCount) {
        dragged = false;
        pressScreenX = screenX;
        pressScreenY = screenY;
        pressX = modelX;
        pressY = modelY;
        currentX = pressX;
        currentY = pressY;
        Tool tool = host.tool();
        Layout.ElementHit hit = host.geometry().hit(pressX, pressY);
        if (tool.edgeKind() != null) {
            if (hit != null && hit.element() instanceof Node node) {
                edgeSource = node;
                edgeSourceHeader = hit.part() == Layout.Part.HEADER;
                mode = Mode.EDGE;
            } else {
                mode = Mode.NONE;
            }
            host.repaintCanvas();
            return;
        }
        if (tool.nodeType() != null) {
            mode = Mode.NONE;
            return;
        }
        clickElement = hit == null ? null : hit.element();
        clickWasSelected = clickElement != null && selected.contains(clickElement);
        if (clickElement == null) {
            if (!control) {
                selected.clear();
            }
            mode = Mode.RUBBER;
        } else if (control) {
            if (!selected.remove(clickElement)) {
                selected.add(clickElement);
            }
            mode = Mode.MOVE;
            captureOrigins();
        } else if (!selected.contains(clickElement)) {
            selected.clear();
            selected.add(clickElement);
            mode = Mode.MOVE;
            captureOrigins();
        } else {
            mode = Mode.MOVE;
            captureOrigins();
        }
        host.repaintCanvas();
    }

    public void drag(int modelX, int modelY, int screenX, int screenY) {
        if (Math.hypot(screenX - pressScreenX, screenY - pressScreenY) > 4) {
            dragged = true;
            pendingCreate = false;
        }
        currentX = modelX;
        currentY = modelY;
        if (mode == Mode.MOVE && dragged) {
            int dx = currentX - pressX;
            int dy = currentY - pressY;
            for (Map.Entry<Node, Point> entry : origins.entrySet()) {
                entry.getKey().moveTo(Geom.snap(entry.getValue().x + dx), Geom.snap(entry.getValue().y + dy));
            }
            host.diagram().touch();
        }
        host.repaintCanvas();
    }

    public void release(int modelX, int modelY, int screenX, int screenY, boolean control, int clickCount) {
        currentX = modelX;
        currentY = modelY;
        if (Math.hypot(screenX - pressScreenX, screenY - pressScreenY) > 4) {
            dragged = true;
        }
        pendingCreate = false;
        Tool tool = host.tool();
        boolean scheduleCreate = tool.nodeType() != null && !dragged && clickCount < 2;
        if (scheduleCreate) {
            pendingX = currentX;
            pendingY = currentY;
            pendingCreate = true;
        } else if (mode == Mode.EDGE) {
            finishEdge();
        } else if (mode == Mode.RUBBER && dragged) {
            finishRubber(control);
        } else if (mode == Mode.MOVE) {
            finishMove();
            if (!dragged && !control && clickWasSelected && clickElement != null) {
                selected.remove(clickElement);
            }
        }
        mode = Mode.NONE;
        edgeSource = null;
        origins.clear();
        host.repaintCanvas();
    }

    public void openAt(int modelX, int modelY) {
        pendingCreate = false;
        Layout.ElementHit hit = host.geometry().hit(modelX, modelY);
        if (hit != null) {
            selected.clear();
            selected.add(hit.element());
            host.openProperties(hit.element());
            host.repaintCanvas();
        }
    }

    public void createPendingNode() {
        if (!pendingCreate) {
            return;
        }
        pendingCreate = false;
        Tool tool = host.tool();
        NodeType type = tool.nodeType();
        if (type == null) {
            return;
        }
        Diagram diagram = host.diagram();
        int x = Geom.snap(Geom.clamp(pendingX, 8, Math.max(8, host.modelWidth() - 40)));
        int y = Geom.snap(Geom.clamp(pendingY, 8, Math.max(8, host.modelHeight() - 40)));
        if (type == NodeType.FIELD) {
            Node object = host.geometry().objectAt(x, y);
            if (object == null) {
                host.notifyUser("A field must be created inside an object.", true);
                return;
            }
            Node field = Node.create(diagram.nextNodeId(), NodeType.FIELD, x, y);
            field.setParent(object);
            host.stack().push(Commands.addNode(diagram, field));
            selected.clear();
            selected.add(field);
            host.changed();
            if (host.autoEdit()) {
                host.openProperties(field);
            }
            return;
        }
        Layout.ElementHit hit = host.geometry().hit(x, y);
        Node parent = null;
        if (hit != null && hit.element() instanceof Node node) {
            if (node.type() == NodeType.PACKAGE) {
                parent = node;
            } else {
                return;
            }
        }
        Node created = Node.create(diagram.nextNodeId(), type, x, y);
        if (parent != null) {
            created.setParent(parent);
        }
        host.stack().push(Commands.addNode(diagram, created));
        selected.clear();
        selected.add(created);
        host.changed();
        if (host.autoEdit() && !created.specs().isEmpty()) {
            host.openProperties(created);
        }
    }

    public void shiftReparent() {
        if (mode != Mode.NONE) {
            return;
        }
        Diagram diagram = host.diagram();
        List<Command> parts = new ArrayList<>();
        for (Node node : selectedRoots()) {
            if (!node.followsPackageShift()) {
                continue;
            }
            Rectangle box = host.geometry().bounds(node);
            Node pkg = host.geometry().topPackageAt(box.x + box.width / 2, box.y + box.height / 2, node);
            Node parent = node.parent() != null && node.parent().type() == NodeType.PACKAGE ? node.parent() : null;
            if (pkg != null && pkg != parent) {
                parts.add(Commands.reparent(diagram, node, node.parent(), pkg));
            } else if (parent != null) {
                parts.add(Commands.reparent(diagram, node, parent, null));
            }
        }
        if (!parts.isEmpty()) {
            host.stack().push(Commands.compound(parts));
            host.changed();
        }
    }

    private void finishEdge() {
        Diagram diagram = host.diagram();
        Layout.ElementHit hit = host.geometry().hit(currentX, currentY);
        Node target = hit != null && hit.element() instanceof Node node ? node : null;
        boolean targetHeader = hit != null && hit.part() == Layout.Part.HEADER;
        EdgeKind kind = host.tool().edgeKind();
        if (kind == null || edgeSource == null) {
            return;
        }
        if (diagram.type() == jetuml.model.DiagramType.SEQUENCE && (kind == EdgeKind.CALL || kind == EdgeKind.RETURN)) {
            SequenceGestures.Result result = kind == EdgeKind.RETURN
                    ? SequenceGestures.createReturn(diagram, edgeSource, target)
                    : SequenceGestures.create(diagram, edgeSource, edgeSourceHeader, target, targetHeader, currentY);
            if (result.check().kind() == Check.Kind.NOTIFY) {
                host.notifyUser(result.check().message(), true);
            } else if (result.command() != null) {
                host.stack().push(result.command());
                selected.clear();
                if (result.focus() != null) {
                    selected.add(result.focus());
                }
                host.changed();
            }
            return;
        }
        if (kind == EdgeKind.NOTE && target == null) {
            if (edgeSource.type() == NodeType.NOTE) {
                Node point = Node.create(diagram.nextNodeId(), NodeType.POINT, Geom.snap(currentX), Geom.snap(currentY));
                Edge edge = new Edge(diagram.nextEdgeId(), EdgeKind.NOTE, edgeSource, point);
                host.stack().push(Commands.compound(List.of(
                        Commands.addNode(diagram, point),
                        Commands.addEdge(diagram, edge))));
                selected.clear();
                selected.add(edge);
                host.changed();
            }
            return;
        }
        if (target == null) {
            return;
        }
        Check check = Rules.canCreate(diagram, kind, edgeSource, target);
        if (!check.ok()) {
            if (check.kind() == Check.Kind.NOTIFY) {
                host.notifyUser(check.message(), true);
            }
            return;
        }
        Edge edge = new Edge(diagram.nextEdgeId(), kind, edgeSource, target);
        host.stack().push(Commands.addEdge(diagram, edge));
        selected.clear();
        selected.add(edge);
        host.changed();
    }

    private void finishRubber(boolean additive) {
        Rectangle region = rubber();
        if (region == null) {
            return;
        }
        if (!additive) {
            selected.clear();
        }
        Diagram diagram = host.diagram();
        for (Node node : diagram.nodes()) {
            if (host.geometry().regionIntersects(node, region)) {
                selected.add(node);
            }
        }
        for (Edge edge : diagram.edges()) {
            if (host.geometry().regionIntersects(edge, region)) {
                selected.add(edge);
            }
        }
    }

    private void finishMove() {
        if (!dragged || origins.isEmpty()) {
            restoreOrigins();
            return;
        }
        boolean changed = false;
        List<Node> nodes = new ArrayList<>(origins.keySet());
        int[] fromX = new int[nodes.size()];
        int[] fromY = new int[nodes.size()];
        int[] toX = new int[nodes.size()];
        int[] toY = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            Point origin = origins.get(node);
            fromX[i] = origin.x;
            fromY[i] = origin.y;
            toX[i] = node.x();
            toY[i] = node.y();
            changed |= fromX[i] != toX[i] || fromY[i] != toY[i];
        }
        if (!changed) {
            return;
        }
        host.stack().push(new Commands.Move(host.diagram(), nodes, fromX, fromY, toX, toY), false);
        host.changed();
    }

    private void restoreOrigins() {
        for (Map.Entry<Node, Point> entry : origins.entrySet()) {
            entry.getKey().moveTo(entry.getValue().x, entry.getValue().y);
        }
        if (!origins.isEmpty()) {
            host.diagram().touch();
        }
    }

    private void captureOrigins() {
        origins.clear();
        for (Node root : selectedRoots()) {
            if (root.movable()) {
                captureTree(root);
            }
        }
    }

    private void captureTree(Node node) {
        origins.put(node, new Point(node.x(), node.y()));
        for (Node child : host.diagram().children(node)) {
            captureTree(child);
        }
    }

    private List<Node> selectedRoots() {
        List<Node> roots = new ArrayList<>();
        for (Element element : selected) {
            if (element instanceof Node node && !selectionContainsAncestor(node)) {
                roots.add(node);
            }
        }
        return roots;
    }

    private boolean selectionContainsAncestor(Node node) {
        for (Node parent = node.parent(); parent != null; parent = parent.parent()) {
            if (selected.contains(parent)) {
                return true;
            }
        }
        return false;
    }

    private Point anchor(Node node) {
        Rectangle box = node.type() == NodeType.IMPLICIT_PARAMETER ? host.geometry().header(node) : host.geometry().bounds(node);
        if (node.type() == NodeType.FIELD) {
            return new Point(box.x + box.width - 4, box.y + box.height / 2);
        }
        return new Point(box.x + box.width / 2, box.y + box.height / 2);
    }
}
