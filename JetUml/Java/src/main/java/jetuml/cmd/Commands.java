package jetuml.cmd;

import java.util.ArrayList;
import java.util.List;

import jetuml.model.Diagram;
import jetuml.model.Edge;
import jetuml.model.Element;
import jetuml.model.Node;

public final class Commands {
    private Commands() {
    }

    public static Command compound(List<Command> parts) {
        return new Command() {
            @Override
            public void execute() {
                for (Command part : parts) {
                    part.execute();
                }
            }

            @Override
            public void undo() {
                for (int i = parts.size() - 1; i >= 0; i--) {
                    parts.get(i).undo();
                }
            }
        };
    }

    public static Command addNode(Diagram diagram, Node node) {
        return new Command() {
            @Override
            public void execute() {
                diagram.addNode(node);
            }

            @Override
            public void undo() {
                diagram.removeNode(node);
            }
        };
    }

    public static Command addEdge(Diagram diagram, Edge edge) {
        return new Command() {
            @Override
            public void execute() {
                diagram.addEdge(edge);
            }

            @Override
            public void undo() {
                diagram.removeEdge(edge);
            }
        };
    }

    public static Command property(Diagram diagram, Element element, String key, String value) {
        String previous = element.get(key);
        return new Command() {
            @Override
            public void execute() {
                element.set(key, value);
                diagram.touch();
            }

            @Override
            public void undo() {
                element.set(key, previous);
                diagram.touch();
            }
        };
    }

    public static final class Move implements Command {
        private final Diagram diagram;
        private final List<Node> nodes;
        private final int[] fromX;
        private final int[] fromY;
        private final int[] toX;
        private final int[] toY;

        public Move(Diagram diagram, List<Node> nodes, int[] fromX, int[] fromY, int[] toX, int[] toY) {
            this.diagram = diagram;
            this.nodes = List.copyOf(nodes);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @Override
        public void execute() {
            apply(toX, toY);
        }

        @Override
        public void undo() {
            apply(fromX, fromY);
        }

        private void apply(int[] xs, int[] ys) {
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).moveTo(xs[i], ys[i]);
            }
            diagram.touch();
        }
    }

    public static Command reparent(Diagram diagram, Node node, Node from, Node to) {
        return new Command() {
            @Override
            public void execute() {
                diagram.setParent(node, to);
            }

            @Override
            public void undo() {
                diagram.setParent(node, from);
            }
        };
    }

    /** Remove nós (e a subárvore), arestas incidentes e âncoras que ficariam órfãs. */
    public static Command delete(Diagram diagram, Iterable<Element> selected) {
        java.util.LinkedHashSet<Node> doomed = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Edge> edgeSet = new java.util.LinkedHashSet<>();
        for (Element element : selected) {
            if (element instanceof Node node) {
                collect(diagram, node, doomed);
            } else if (element instanceof Edge edge) {
                edgeSet.add(edge);
                if (edge.kind() == jetuml.model.EdgeKind.CALL && edge.end() != null) {
                    collect(diagram, edge.end(), doomed);
                }
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Node node : new ArrayList<>(doomed)) {
                for (Edge edge : diagram.edgesTouching(node)) {
                    if (edge.kind() == jetuml.model.EdgeKind.CALL && edge.start() == node && doomed.add(edge.end())) {
                        collect(diagram, edge.end(), doomed);
                        grew = true;
                    }
                    edgeSet.add(edge);
                }
            }
        }
        for (Edge edge : new ArrayList<>(edgeSet)) {
            if (edge.end() != null && edge.end().type() == jetuml.model.NodeType.POINT) {
                doomed.add(edge.end());
            }
            if (edge.start() != null && edge.start().type() == jetuml.model.NodeType.POINT) {
                doomed.add(edge.start());
            }
        }
        List<Node> nodeList = new ArrayList<>();
        List<Integer> nodeIndex = new ArrayList<>();
        for (Node node : diagram.nodes()) {
            if (doomed.contains(node)) {
                nodeList.add(node);
                nodeIndex.add(diagram.indexOf(node));
            }
        }
        List<Edge> edgeList = new ArrayList<>();
        List<Integer> edgeIndex = new ArrayList<>();
        for (Edge edge : diagram.edges()) {
            if (edgeSet.contains(edge) || doomed.contains(edge.start()) || doomed.contains(edge.end())) {
                edgeList.add(edge);
                edgeIndex.add(diagram.indexOf(edge));
            }
        }
        return new Command() {
            @Override
            public void execute() {
                for (int i = edgeList.size() - 1; i >= 0; i--) {
                    diagram.removeEdge(edgeList.get(i));
                }
                for (int i = nodeList.size() - 1; i >= 0; i--) {
                    diagram.removeNode(nodeList.get(i));
                }
            }

            @Override
            public void undo() {
                for (int i = 0; i < nodeList.size(); i++) {
                    diagram.addNode(nodeIndex.get(i), nodeList.get(i));
                }
                for (int i = 0; i < edgeList.size(); i++) {
                    diagram.addEdge(edgeIndex.get(i), edgeList.get(i));
                }
            }
        };
    }

    private static void collect(Diagram diagram, Node node, java.util.Set<Node> doomed) {
        if (node == null || !doomed.add(node)) {
            return;
        }
        for (Node child : diagram.children(node)) {
            collect(diagram, child, doomed);
        }
    }
}
