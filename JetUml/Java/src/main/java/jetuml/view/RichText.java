package jetuml.view;

import java.util.ArrayList;
import java.util.List;

/** /itálico/ e _sublinhado_, inclusive aninhados (RF24). */
public final class RichText {
    private RichText() {
    }

    public record Segment(String text, boolean italic, boolean underline) {
    }

    public static List<Segment> parse(String line) {
        List<Segment> segments = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return segments;
        }
        parseInto(line, false, false, segments);
        return segments;
    }

    private static void parseInto(String text, boolean italic, boolean underline, List<Segment> out) {
        int index = 0;
        while (index < text.length()) {
            char mark = text.charAt(index);
            if (mark == '/' || mark == '_') {
                int close = text.indexOf(mark, index + 1);
                if (close > index) {
                    parseInto(text.substring(index + 1, close), italic || mark == '/', underline || mark == '_', out);
                    index = close + 1;
                    continue;
                }
            }
            int next = index + 1;
            while (next < text.length()) {
                char candidate = text.charAt(next);
                if ((candidate == '/' || candidate == '_') && text.indexOf(candidate, next + 1) > next) {
                    break;
                }
                next++;
            }
            String chunk = text.substring(index, next);
            if (!chunk.isEmpty()) {
                out.add(new Segment(chunk, italic, underline));
            }
            index = next;
        }
    }
}
