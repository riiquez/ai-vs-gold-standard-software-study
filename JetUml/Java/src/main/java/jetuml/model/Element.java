package jetuml.model;

import java.util.List;

/** Nó ou aresta selecionável e editável. */
public interface Element {
    String id();

    List<PropertySpec> specs();

    String get(String key);

    void set(String key, String value);
}
