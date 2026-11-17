package jetuml.view;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;

import jetuml.model.Diagram;

/** Imagem do diagrama sem grade, cortada no conteúdo (RF04 e RF05). */
public final class Exporter {
    private static final int MARGIN = 16;
    private static final DiagramPainter PAINTER = new DiagramPainter();

    private Exporter() {
    }

    public static BufferedImage image(Diagram diagram, Font font, boolean dark) {
        Measure measure = new Measure(font);
        Layout layout = Layout.of(diagram, measure);
        Rectangle content = layout.contentBounds();
        int width = Math.max(1, content.width + MARGIN * 2);
        int height = Math.max(1, content.height + MARGIN * 2);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Theme theme = new Theme(dark);
        graphics.setColor(theme.canvas());
        graphics.fillRect(0, 0, width, height);
        graphics.translate(-content.x + MARGIN, -content.y + MARGIN);
        PAINTER.paint(new SwingDraw(graphics), layout, theme, false, 0, 0, Set.of(), null, null);
        graphics.dispose();
        return image;
    }

    public static String svg(Diagram diagram, Font font, boolean dark) {
        Measure measure = new Measure(font);
        Layout layout = Layout.of(diagram, measure);
        Rectangle content = layout.contentBounds();
        int width = Math.max(1, content.width + MARGIN * 2);
        int height = Math.max(1, content.height + MARGIN * 2);
        Theme theme = new Theme(dark);
        SvgDraw draw = new SvgDraw();
        PAINTER.paint(draw, layout, theme, false, 0, 0, Set.of(), null, null);
        return draw.document(width, height, theme.canvas(), -content.x + MARGIN, -content.y + MARGIN);
    }
}
