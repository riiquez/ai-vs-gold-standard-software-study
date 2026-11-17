package jetuml.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jetuml.cmd.Command;
import jetuml.cmd.Commands;
import jetuml.model.Diagram;
import jetuml.model.DiagramType;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Element;
import jetuml.model.Geom;
import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.rules.Check;

/** Área de transferência interna de elementos, independente da imagem do sistema. */
public final class ElementClipboard {
    private static Snapshot snapshot;
    private static int pasteSerial = 1;

    private ElementClipboard() {
    }

    public static boolean hasContent() {
        return snapshot != null && !snapshot.nodes.isEmpty();
    }

    public static void copy(Diagram diagram, Iterable<Element> selected) {
        LinkedHashSet<Node> nodes = new LinkedHashSet<>();
        LinkedHashSet<Edge> edges = new LinkedHashSet<>();
        for (Element element : selected) {
            if (element instanceof Node node) {
                addTree(diagram, node, nodes);
            } else if (element instanceof Edge edge) {
                edges.add(edge);
            }
        }
        for (Edge edge : new ArrayList<>(edges)) {
            if (edge.start() != null) {
                nodes.add(edge.start());
            }
            if (edge.end() != null) {
                nodes.add(edge.end());
            }
        }
        for (Edge edge : diagram.edges()) {
            if (nodes.contains(edge.start()) && nodes.contains(edge.end())) {
                edges.add(edge);
            }
        }
        if (nodes.isEmpty() && edges.isEmpty()) {
            return;
        }
        List<StoredNode> storedNodes = new ArrayList<>();
        for (Node node : nodes) {
            String parentId = node.parent() != null && nodes.contains(node.parent()) ? node.parent().id() : null;
            storedNodes.add(new StoredNode(node.id(), node.type(), node.x(), node.y(), parentId, Map.copyOf(node.properties())));
        }
        List<StoredEdge> storedEdges = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.start() == null || edge.end() == null) {
                continue;
            }
            if (!nodes.contains(edge.start()) || !nodes.contains(edge.end())) {
                continue;
            }
            storedEdges.add(new StoredEdge(edge.kind(), edge.start().id(), edge.end().id(), Map.copyOf(edge.properties()), edge.isConstructor()));
        }
        snapshot = new Snapshot(storedNodes, storedEdges);
        pasteSerial = 1;
    }

    public static Paste paste(Diagram target) {
        if (snapshot == null || snapshot.nodes.isEmpty()) {
            return Paste.fail(Check.notify("Clipboard is empty."));
        }
        if (target.type() == DiagramType.SEQUENCE) {
            return Paste.fail(Check.notify("Pasting into a sequence diagram is not allowed."));
        }
        for (StoredNode node : snapshot.nodes) {
            if (!target.type().accepts(node.type)) {
                return Paste.fail(Check.notify("These elements are not compatible with the destination diagram."));
            }
        }
        for (StoredEdge edge : snapshot.edges) {
            if (!target.type().accepts(edge.kind)) {
                return Paste.fail(Check.notify("These elements are not compatible with the destination diagram."));
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        for (StoredNode node : snapshot.nodes) {
            ids.add(node.id);
        }
        for (StoredNode node : snapshot.nodes) {
            if (node.type == NodeType.FIELD) {
                StoredNode parent = find(node.parentId);
                if (parent == null || parent.type != NodeType.OBJECT || !ids.contains(node.parentId)) {
                    return Paste.fail(Check.notify("A field must belong to an object."));
                }
            }
        }
        int offset = Geom.GRID * 2 * pasteSerial;
        pasteSerial++;
        Map<String, Node> created = new LinkedHashMap<>();
        List<Command> parts = new ArrayList<>();
        List<Element> selected = new ArrayList<>();
        for (StoredNode stored : snapshot.nodes) {
            Node node = Node.create(target.nextNodeId(), stored.type, Geom.snap(stored.x + offset), Geom.snap(stored.y + offset));
            stored.props.forEach(node::set);
            created.put(stored.id, node);
            parts.add(Commands.addNode(target, node));
            selected.add(node);
        }
        for (StoredNode stored : snapshot.nodes) {
            if (stored.parentId != null && created.containsKey(stored.parentId)) {
                Node child = created.get(stored.id);
                Node parent = created.get(stored.parentId);
                parts.add(Commands.reparent(target, child, null, parent));
            }
        }
        for (StoredEdge stored : snapshot.edges) {
            Node start = created.get(stored.startId);
            Node end = created.get(stored.endId);
            if (start == null || end == null) {
                continue;
            }
            Edge edge = new Edge(target.nextEdgeId(), stored.kind, start, end);
            stored.props.forEach((key, value) -> {
                if (!"constructor".equals(key)) {
                    edge.set(key, value);
                }
            });
            if (stored.constructor) {
                edge.setConstructor(true);
            }
            parts.add(Commands.addEdge(target, edge));
            selected.add(edge);
        }
        return new Paste(Check.pass(), Commands.compound(parts), selected);
    }

    private static StoredNode find(String id) {
        if (id == null || snapshot == null) {
            return null;
        }
        for (StoredNode node : snapshot.nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private static void addTree(Diagram diagram, Node node, Set<Node> nodes) {
        if (!nodes.add(node)) {
            return;
        }
        for (Node child : diagram.children(node)) {
            addTree(diagram, child, nodes);
        }
    }

    public record Paste(Check check, Command command, List<Element> elements) {
        static Paste fail(Check check) {
            return new Paste(check, null, List.of());
        }
    }

    private record Snapshot(List<StoredNode> nodes, List<StoredEdge> edges) {
    }

    private record StoredNode(String id, NodeType type, int x, int y, String parentId, Map<String, String> props) {
    }

    private record StoredEdge(EdgeKind kind, String startId, String endId, Map<String, String> props, boolean constructor) {
    }
}
