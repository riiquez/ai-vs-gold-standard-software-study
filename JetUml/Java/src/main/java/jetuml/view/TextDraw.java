package jetuml.view;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

/** Desenho de texto rico compartilhado pelos nós e pelos decoradores de aresta. */
public final class TextDraw {
    private TextDraw() {
    }

    public static List<String> blank(List<String> lines) {
        return lines.isEmpty() ? List.of("") : lines;
    }

    public static void rows(Draw2D g, Measure measure, List<String> rows, int x, int top, Color color) {
        int baseline = top + measure.ascent();
        for (String row : rows) {
            rich(g, measure, row, x, baseline, color);
            baseline += measure.lineHeight();
        }
    }

    public static void centered(Draw2D g, Measure measure, String text, int x, int top, int width, Color color) {
        centered(g, measure, text, x, top, width, color, false);
    }

    public static void centered(Draw2D g, Measure measure, String text, int x, int top, int width, Color color,
            boolean underline) {
        int textWidth = measure.width(text);
        int start = x + Math.max(4, (width - textWidth) / 2);
        int baseline = top + measure.ascent();
        rich(g, measure, text, start, baseline, color);
        if (underline) {
            g.line(start, baseline + 2, start + textWidth, baseline + 2, color, 1f, false);
        }
    }

    public static void rich(Draw2D g, Measure measure, String text, int x, int baseline, Color color) {
        int cursor = x;
        for (RichText.Segment segment : RichText.parse(text)) {
            Font font = segment.italic() ? measure.italicFont() : measure.font();
            g.text(segment.text(), cursor, baseline, font, color);
            int width = segment.italic()
                    ? new Measure(measure.italicFont()).plainWidth(segment.text())
                    : measure.plainWidth(segment.text());
            if (segment.underline()) {
                g.line(cursor, baseline + 2, cursor + width, baseline + 2, color, 1f, false);
            }
            cursor += width;
        }
    }
}
