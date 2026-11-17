package jetuml.persist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jetuml.model.Diagram;
import jetuml.model.DiagramType;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Node;
import jetuml.model.NodeType;

/** Codec do formato jetuml-1. Campos desconhecidos são ignorados na leitura. */
public final class DiagramCodec {
    public static final String FORMAT = "jetuml-1";

    private DiagramCodec() {
    }

    public static String encode(Diagram diagram) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", FORMAT);
        root.put("diagramType", diagram.type().json());
        List<Object> nodes = new ArrayList<>();
        for (Node node : diagram.nodes()) {
            nodes.add(encodeNode(node));
        }
        List<Object> edges = new ArrayList<>();
        for (Edge edge : diagram.edges()) {
            edges.add(encodeEdge(edge));
        }
        root.put("nodes", nodes);
        root.put("edges", edges);
        return Json.write(root);
    }

    private static Map<String, Object> encodeNode(Node node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.id());
        map.put("type", node.type().json());
        map.put("x", node.x());
        map.put("y", node.y());
        map.put("parent", node.parent() == null ? null : node.parent().id());
        for (Map.Entry<String, String> entry : node.properties().entrySet()) {
            if ("openBottom".equals(entry.getKey())) {
                map.put(entry.getKey(), "true".equals(entry.getValue()));
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    private static Map<String, Object> encodeEdge(Edge edge) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", edge.id());
        map.put("type", edge.kind().json());
        map.put("start", edge.start().id());
        map.put("end", edge.end().id());
        for (String key : List.of("startLabel", "middleLabel", "endLabel", "directionality",
                "aggregationType", "generalizationType", "dependencyType")) {
            if (!edge.get(key).isEmpty()) {
                map.put(key, edge.get(key));
            }
        }
        if (edge.kind() == EdgeKind.CALL) {
            map.put("constructor", edge.isConstructor());
        }
        return map;
    }

    public static Diagram decode(String json) {
        Map<String, Object> root = Json.obj(Json.parse(json));
        DiagramType type = DiagramType.fromJson(Json.text(root.get("diagramType"), ""));
        if (type == null) {
            throw new IllegalArgumentException("Unknown or missing diagramType.");
        }
        Diagram diagram = new Diagram(type);
        List<Node> created = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        for (Object item : Json.array(root.get("nodes"))) {
            Map<String, Object> map = Json.obj(item);
            NodeType nodeType = NodeType.fromJson(Json.text(map.get("type"), ""));
            if (nodeType == null) {
                throw new IllegalArgumentException("Unknown node type: " + map.get("type"));
            }
            String id = Json.text(map.get("id"), "");
            if (id.isBlank() || diagram.node(id) != null) {
                throw new IllegalArgumentException("Missing or duplicated node id.");
            }
            Node node = Node.create(id, nodeType, Json.integer(map.get("x"), 0), Json.integer(map.get("y"), 0));
            for (String key : node.properties().keySet()) {
                if (!map.containsKey(key) || "id".equals(key) || "type".equals(key)) {
                    continue;
                }
                Object value = map.get(key);
                if (value instanceof Boolean flag) {
                    node.set(key, flag ? "true" : "false");
                } else if (value != null && !(value instanceof Map) && !(value instanceof List)) {
                    node.set(key, String.valueOf(value));
                }
            }
            diagram.addNode(node);
            created.add(node);
            Object parent = map.get("parent");
            parents.add(parent == null ? null : String.valueOf(parent));
        }
        for (int i = 0; i < created.size(); i++) {
            String parentId = parents.get(i);
            if (parentId == null || parentId.isBlank() || "null".equals(parentId)) {
                continue;
            }
            Node parent = diagram.node(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("Unknown parent " + parentId);
            }
            created.get(i).setParent(parent);
        }
        for (Node node : diagram.nodes()) {
            if (node.type() == NodeType.FIELD && (node.parent() == null || node.parent().type() != NodeType.OBJECT)) {
                throw new IllegalArgumentException("A field must belong to an object.");
            }
            if (node.type() == NodeType.CALL && (node.parent() == null || node.parent().type() != NodeType.IMPLICIT_PARAMETER)) {
                throw new IllegalArgumentException("An activation box must belong to a lifeline.");
            }
        }
        for (Object item : Json.array(root.get("edges"))) {
            Map<String, Object> map = Json.obj(item);
            EdgeKind kind = EdgeKind.fromJson(Json.text(map.get("type"), ""));
            if (kind == null) {
                throw new IllegalArgumentException("Unknown edge type: " + map.get("type"));
            }
            Node start = diagram.node(Json.text(map.get("start"), ""));
            Node end = diagram.node(Json.text(map.get("end"), ""));
            if (start == null || end == null) {
                throw new IllegalArgumentException("An edge points to a missing node.");
            }
            String id = Json.text(map.get("id"), diagram.nextEdgeId());
            Edge edge = new Edge(id, kind, start, end);
            for (String key : List.of("startLabel", "middleLabel", "endLabel", "directionality",
                    "aggregationType", "generalizationType", "dependencyType")) {
                if (map.containsKey(key) && map.get(key) != null) {
                    edge.set(key, String.valueOf(map.get(key)));
                }
            }
            if (map.containsKey("constructor")) {
                edge.setConstructor(Json.bool(map.get("constructor"), false));
            }
            diagram.addEdge(edge);
        }
        return diagram;
    }
}
