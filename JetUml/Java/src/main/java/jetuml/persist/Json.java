package jetuml.persist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Leitor e escritor JSON sem dependências, suficiente para o formato aberto do JetUML. */
public final class Json {
    private Json() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        out.append('\n');
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder out, int indent) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            out.append('"').append(escape(text)).append('"');
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Double number) {
            if (number == Math.rint(number) && !number.isInfinite()) {
                out.append(number.longValue());
            } else {
                out.append(number);
            }
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            if (!map.isEmpty()) {
                out.append('\n');
                int i = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    indent(out, indent + 1);
                    out.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
                    write(entry.getValue(), out, indent + 1);
                    if (++i < map.size()) {
                        out.append(',');
                    }
                    out.append('\n');
                }
                indent(out, indent);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            if (!list.isEmpty()) {
                out.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    indent(out, indent + 1);
                    write(list.get(i), out, indent + 1);
                    if (i + 1 < list.size()) {
                        out.append(',');
                    }
                    out.append('\n');
                }
                indent(out, indent);
            }
            out.append(']');
        } else {
            out.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void indent(StringBuilder out, int level) {
        out.append("  ".repeat(level));
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    public static Map<String, Object> obj(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copy;
        }
        throw new IllegalArgumentException("Expected a JSON object.");
    }

    public static List<Object> array(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of();
    }

    public static String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    public static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    public static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text);
        }
        return fallback;
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skip();
            Object value = read();
            skip();
            if (index < text.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object read() {
            skip();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char c = text.charAt(index);
            if (c == '{') {
                return readObject();
            }
            if (c == '[') {
                return readArray();
            }
            if (c == '"') {
                return readString();
            }
            if (c == 't' || c == 'f') {
                return readLiteral();
            }
            if (c == 'n') {
                expect("null");
                return null;
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                return readNumber();
            }
            throw error("Unexpected character " + c);
        }

        private Map<String, Object> readObject() {
            index++;
            Map<String, Object> map = new LinkedHashMap<>();
            skip();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                skip();
                String key = readString();
                skip();
                if (index >= text.length() || text.charAt(index) != ':') {
                    throw error("Expected ':'");
                }
                index++;
                map.put(key, read());
                skip();
                if (peek('}')) {
                    index++;
                    return map;
                }
                if (index >= text.length() || text.charAt(index) != ',') {
                    throw error("Expected ',' or '}'");
                }
                index++;
            }
        }

        private List<Object> readArray() {
            index++;
            List<Object> list = new ArrayList<>();
            skip();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(read());
                skip();
                if (peek(']')) {
                    index++;
                    return list;
                }
                if (index >= text.length() || text.charAt(index) != ',') {
                    throw error("Expected ',' or ']'");
                }
                index++;
            }
        }

        private String readString() {
            if (index >= text.length() || text.charAt(index) != '"') {
                throw error("Expected a string");
            }
            index++;
            StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (index >= text.length()) {
                        throw error("Bad escape");
                    }
                    char e = text.charAt(index++);
                    switch (e) {
                        case '"', '\\', '/' -> out.append(e);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (index + 4 > text.length()) {
                                throw error("Bad unicode escape");
                            }
                            int code = Integer.parseInt(text.substring(index, index + 4), 16);
                            out.append((char) code);
                            index += 4;
                        }
                        default -> throw error("Bad escape");
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private Object readLiteral() {
            if (text.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw error("Expected a boolean");
        }

        private void expect(String literal) {
            if (!text.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
        }

        private Number readNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            boolean fraction = false;
            if (peek('.')) {
                fraction = true;
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                fraction = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String raw = text.substring(start, index);
            if (!fraction && raw.length() < 10) {
                return Integer.valueOf(raw);
            }
            return Double.valueOf(raw);
        }

        private boolean peek(char c) {
            return index < text.length() && text.charAt(index) == c;
        }

        private void skip() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at index " + index);
        }
    }
}
