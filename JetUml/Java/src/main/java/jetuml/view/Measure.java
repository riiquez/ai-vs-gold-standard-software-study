package jetuml.view;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Medição de texto independente de um componente Swing, para funcionar também em SVG. */
public final class Measure {
    private final Font font;
    private final Font italic;
    private final FontMetrics plainMetrics;
    private final FontMetrics italicMetrics;

    public Measure(Font font) {
        this.font = font;
        this.italic = font.deriveFont(Font.ITALIC);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(font);
        this.plainMetrics = graphics.getFontMetrics(font);
        this.italicMetrics = graphics.getFontMetrics(italic);
        graphics.dispose();
    }

    public Font font() {
        return font;
    }

    public Font italicFont() {
        return italic;
    }

    public int lineHeight() {
        return plainMetrics.getHeight();
    }

    public int ascent() {
        return plainMetrics.getAscent();
    }

    public int width(String richLine) {
        int width = 0;
        for (RichText.Segment segment : RichText.parse(richLine)) {
            FontMetrics metrics = segment.italic() ? italicMetrics : plainMetrics;
            width += metrics.stringWidth(segment.text());
        }
        return width;
    }

    public int plainWidth(String text) {
        return plainMetrics.stringWidth(text);
    }

    public static FontRenderContext context() {
        return new FontRenderContext(new AffineTransform(), true, true);
    }

    public List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        for (String line : text.split("\n", -1)) {
            lines.add(line);
        }
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty() && text.endsWith("\n")) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}
