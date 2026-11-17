package jetuml.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Grafo de um diagrama. Nós ficam numa lista plana; a hierarquia é o campo parent. */
public final class Diagram {
    private final DiagramType type;
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, Node> nodesById = new HashMap<>();
    private final Map<String, Edge> edgesById = new HashMap<>();
    private final List<DiagramListener> listeners = new ArrayList<>();
    private int sequence = 1;
    private int revision;

    public Diagram(DiagramType type) {
        this.type = type;
    }

    public DiagramType type() {
        return type;
    }

    public int revision() {
        return revision;
    }

    public void addListener(DiagramListener listener) {
        listeners.add(listener);
    }

    public void touch() {
        revision++;
        for (DiagramListener listener : List.copyOf(listeners)) {
            listener.diagramChanged(this);
        }
    }

    public List<Node> nodes() {
        return List.copyOf(nodes);
    }

    public List<Edge> edges() {
        return List.copyOf(edges);
    }

    public String nextNodeId() {
        return next("n");
    }

    public String nextEdgeId() {
        return next("e");
    }

    private String next(String prefix) {
        String id;
        do {
            id = prefix + sequence++;
        } while (nodesById.containsKey(id) || edgesById.containsKey(id));
        return id;
    }

    public void observeId(String id) {
        if (id != null && id.length() > 1 && (id.charAt(0) == 'n' || id.charAt(0) == 'e')) {
            try {
                int value = Integer.parseInt(id.substring(1));
                sequence = Math.max(sequence, value + 1);
            } catch (NumberFormatException ignored) {
                sequence++;
            }
        }
    }

    public Node node(String id) {
        return nodesById.get(id);
    }

    public void addNode(Node node) {
        addNode(nodes.size(), node);
    }

    public void addNode(int index, Node node) {
        int at = Math.max(0, Math.min(index, nodes.size()));
        nodes.add(at, node);
        nodesById.put(node.id(), node);
        observeId(node.id());
        touch();
    }

    public void removeNode(Node node) {
        nodes.remove(node);
        nodesById.remove(node.id());
        touch();
    }

    public int indexOf(Node node) {
        return nodes.indexOf(node);
    }

    public void addEdge(Edge edge) {
        addEdge(edges.size(), edge);
    }

    public void addEdge(int index, Edge edge) {
        int at = Math.max(0, Math.min(index, edges.size()));
        edges.add(at, edge);
        edgesById.put(edge.id(), edge);
        observeId(edge.id());
        touch();
    }

    public void removeEdge(Edge edge) {
        edges.remove(edge);
        edgesById.remove(edge.id());
        touch();
    }

    public int indexOf(Edge edge) {
        return edges.indexOf(edge);
    }

    public List<Node> roots() {
        List<Node> roots = new ArrayList<>();
        for (Node node : nodes) {
            if (node.parent() == null) {
                roots.add(node);
            }
        }
        return roots;
    }

    public List<Node> children(Node parent) {
        List<Node> children = new ArrayList<>();
        for (Node node : nodes) {
            if (node.parent() == parent) {
                children.add(node);
            }
        }
        return children;
    }

    public List<Node> paintOrder() {
        List<Node> order = new ArrayList<>();
        for (Node root : roots()) {
            walk(root, order);
        }
        return order;
    }

    private void walk(Node node, List<Node> order) {
        order.add(node);
        for (Node child : children(node)) {
            walk(child, order);
        }
    }

    public void setParent(Node node, Node parent) {
        node.setParent(parent);
        touch();
    }

    public boolean isAncestor(Node ancestor, Node node) {
        for (Node current = node; current != null; current = current.parent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    public List<Edge> edgesTouching(Node node) {
        List<Edge> found = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.start() == node || edge.end() == node) {
                found.add(edge);
            }
        }
        return found;
    }

    public Node callerOf(Node call) {
        for (Edge edge : edges) {
            if (edge.kind() == EdgeKind.CALL && edge.end() == call) {
                return edge.start();
            }
        }
        return null;
    }

    public int rootActivations() {
        int count = 0;
        for (Node node : nodes) {
            if (node.type() == NodeType.CALL && callerOf(node) == null) {
                count++;
            }
        }
        return count;
    }
}
