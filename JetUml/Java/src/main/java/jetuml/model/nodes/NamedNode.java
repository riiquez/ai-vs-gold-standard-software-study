package jetuml.model.nodes;

import java.util.List;
import java.util.Map;

import jetuml.model.Node;
import jetuml.model.PropertySpec;

/** Nó cuja única propriedade de texto é o nome. */
public abstract class NamedNode extends Node {
    protected NamedNode(String id, int x, int y) {
        super(id, x, y);
    }

    protected abstract String defaultName();

    @Override
    protected Map<String, String> defaultProperties() {
        return Map.of("name", defaultName());
    }

    @Override
    public List<PropertySpec> specs() {
        return List.of(PropertySpec.text("name", "Name"));
    }
}
