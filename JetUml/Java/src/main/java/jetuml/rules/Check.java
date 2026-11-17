package jetuml.rules;

/** Resultado de uma tentativa de edição. SILENT não gera notificação (RF30). */
public record Check(Kind kind, String message) {
    public enum Kind { OK, NOTIFY, SILENT }

    public boolean ok() {
        return kind == Kind.OK;
    }

    public static Check pass() {
        return new Check(Kind.OK, "");
    }

    public static Check notify(String message) {
        return new Check(Kind.NOTIFY, message);
    }

    public static Check silent() {
        return new Check(Kind.SILENT, "");
    }
}
