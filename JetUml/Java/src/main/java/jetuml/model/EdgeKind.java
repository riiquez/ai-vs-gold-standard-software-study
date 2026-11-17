package jetuml.model;

import java.util.List;

public enum EdgeKind {
    DEPENDENCY("Dependency"),
    INHERITANCE("Inheritance"),
    REALIZATION("Realization"),
    ASSOCIATION("Association"),
    AGGREGATION("Aggregation"),
    COMPOSITION("Composition"),
    NOTE("NoteConnector"),
    CALL("Call"),
    RETURN("Return"),
    TRANSITION("Transition"),
    REFERENCE("Reference"),
    COLLABORATION("Collaboration"),
    USE_ASSOCIATION("UseCaseAssociation"),
    INCLUDE("Include"),
    EXTEND("Extend"),
    GENERALIZATION("Generalization"),
    USE_DEPENDENCY("UseCaseDependency");

    private final String json;

    EdgeKind(String json) {
        this.json = json;
    }

    public String json() {
        return json;
    }

    public static EdgeKind fromJson(String value) {
        for (EdgeKind kind : values()) {
            if (kind.json.equals(value)) {
                return kind;
            }
        }
        return null;
    }

    public boolean isClassCycleKind() {
        return this == INHERITANCE || this == DEPENDENCY || this == AGGREGATION || this == ASSOCIATION;
    }

    public boolean isAggregationFamily() {
        return this == AGGREGATION || this == COMPOSITION;
    }

    public List<PropertySpec> specs() {
        PropertySpec start = PropertySpec.text("startLabel", "Start Label");
        PropertySpec middle = PropertySpec.text("middleLabel", "Middle Label");
        PropertySpec end = PropertySpec.text("endLabel", "End Label");
        PropertySpec direction3 = PropertySpec.choice("directionality", "Directionality",
                "Unspecified", "Unidirectional", "Bidirectional");
        PropertySpec direction2 = PropertySpec.choice("directionality", "Directionality",
                "Unidirectional", "Bidirectional");
        PropertySpec aggregation = PropertySpec.choice("aggregationType", "Aggregation Type",
                "Aggregation", "Composition");
        PropertySpec generalization = PropertySpec.choice("generalizationType", "Generalization Type",
                "Dependency", "Inheritance", "Implementation");
        PropertySpec dependency = PropertySpec.choice("dependencyType", "Dependency Type",
                "None", "Include", "Extend");
        return switch (this) {
            case ASSOCIATION -> List.of(start, middle, end, direction3);
            case AGGREGATION, COMPOSITION -> List.of(start, middle, end, direction3, aggregation);
            case DEPENDENCY, INHERITANCE, REALIZATION -> List.of(generalization, direction2);
            case CALL, RETURN, COLLABORATION, TRANSITION -> List.of(middle);
            case INCLUDE, EXTEND, USE_DEPENDENCY -> List.of(dependency);
            default -> List.of();
        };
    }

    public static EdgeKind fromGeneralization(String value) {
        return switch (value) {
            case "Implementation" -> REALIZATION;
            case "Dependency" -> DEPENDENCY;
            default -> INHERITANCE;
        };
    }

    public static EdgeKind fromAggregation(String value) {
        return "Composition".equals(value) ? COMPOSITION : AGGREGATION;
    }

    public static EdgeKind fromDependency(String value) {
        return switch (value) {
            case "Include" -> INCLUDE;
            case "Extend" -> EXTEND;
            default -> USE_DEPENDENCY;
        };
    }
}
