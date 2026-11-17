package jetuml.model;

import java.util.List;

/** Campo exibido no painel de propriedades. */
public record PropertySpec(String key, String label, Kind kind, List<String> options) {
    public enum Kind { TEXT, MULTILINE, CHOICE, BOOLEAN }

    public static PropertySpec text(String key, String label) {
        return new PropertySpec(key, label, Kind.TEXT, List.of());
    }

    public static PropertySpec multi(String key, String label) {
        return new PropertySpec(key, label, Kind.MULTILINE, List.of());
    }

    public static PropertySpec choice(String key, String label, String... options) {
        return new PropertySpec(key, label, Kind.CHOICE, List.of(options));
    }

    public static PropertySpec bool(String key, String label) {
        return new PropertySpec(key, label, Kind.BOOLEAN, List.of());
    }
}
