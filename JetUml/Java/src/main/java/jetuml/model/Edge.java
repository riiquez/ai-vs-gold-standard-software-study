package jetuml.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aresta única. O tipo pode mudar pela propriedade (agregação vira composição, por exemplo),
 * então a figura não vira outra classe. O desenho compõe decoradores em cima desta aresta.
 */
public final class Edge implements Element {
    private final String id;
    private EdgeKind kind;
    private Node start;
    private Node end;
    private final Map<String, String> props = new LinkedHashMap<>();

    public Edge(String id, EdgeKind kind, Node start, Node end) {
        this.id = id;
        this.kind = kind;
        this.start = start;
        this.end = end;
        props.put("startLabel", "");
        props.put("middleLabel", "");
        props.put("endLabel", "");
        switch (kind) {
            case ASSOCIATION -> props.put("directionality", "Unspecified");
            case AGGREGATION -> {
                props.put("directionality", "Unspecified");
                props.put("aggregationType", "Aggregation");
            }
            case COMPOSITION -> {
                props.put("directionality", "Unspecified");
                props.put("aggregationType", "Composition");
            }
            case INHERITANCE -> {
                props.put("directionality", "Unidirectional");
                props.put("generalizationType", "Inheritance");
            }
            case REALIZATION -> {
                props.put("directionality", "Unidirectional");
                props.put("generalizationType", "Implementation");
            }
            case DEPENDENCY -> {
                props.put("directionality", "Unidirectional");
                props.put("generalizationType", "Dependency");
            }
            case INCLUDE -> props.put("dependencyType", "Include");
            case EXTEND -> props.put("dependencyType", "Extend");
            case USE_DEPENDENCY -> props.put("dependencyType", "None");
            case CALL -> props.put("constructor", "false");
            default -> {
            }
        }
    }

    @Override
    public String id() {
        return id;
    }

    public EdgeKind kind() {
        return kind;
    }

    public Node start() {
        return start;
    }

    public Node end() {
        return end;
    }

    public void setStart(Node start) {
        this.start = start;
    }

    public void setEnd(Node end) {
        this.end = end;
    }

    public boolean isConstructor() {
        return "true".equals(props.get("constructor"));
    }

    public void setConstructor(boolean value) {
        props.put("constructor", value ? "true" : "false");
        if (value && (get("middleLabel").isBlank())) {
            props.put("middleLabel", "\u00ABcreate\u00BB");
        }
    }

    @Override
    public List<PropertySpec> specs() {
        return kind.specs();
    }

    @Override
    public String get(String key) {
        return props.getOrDefault(key, "");
    }

    @Override
    public void set(String key, String value) {
        props.put(key, value == null ? "" : value);
        switch (key) {
            case "aggregationType" -> kind = EdgeKind.fromAggregation(value);
            case "generalizationType" -> {
                kind = EdgeKind.fromGeneralization(value);
                if ("Unspecified".equals(get("directionality")) || get("directionality").isBlank()) {
                    props.put("directionality", "Unidirectional");
                }
            }
            case "dependencyType" -> kind = EdgeKind.fromDependency(value);
            default -> {
            }
        }
    }

    public Map<String, String> properties() {
        return props;
    }
}
