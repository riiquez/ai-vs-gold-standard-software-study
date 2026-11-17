package jetuml.view;

import java.awt.Color;
import java.awt.Font;

/** SVG real, com as mesmas primitivas do desenho na tela. */
public final class SvgDraw implements Draw2D {
    private final StringBuilder body = new StringBuilder();

    @Override
    public void line(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed) {
        body.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2).append("\" ")
                .append(stroke(color, width, dashed)).append("/>\n");
    }

    @Override
    public void polyline(int[] xs, int[] ys, Color color, float width, boolean dashed) {
        body.append("<polyline points=\"");
        for (int i = 0; i < xs.length; i++) {
            if (i > 0) {
                body.append(' ');
            }
            body.append(xs[i]).append(',').append(ys[i]);
        }
        body.append("\" fill=\"none\" ").append(stroke(color, width, dashed)).append("/>\n");
    }

    @Override
    public void rect(int x, int y, int w, int h, Color fill, Color stroke, float width) {
        body.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(w).append("\" height=\"").append(h).append("\" ")
                .append(paint(fill, stroke, width)).append("/>\n");
    }

    @Override
    public void roundRect(int x, int y, int w, int h, int arc, Color fill, Color stroke, float width) {
        body.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(w).append("\" height=\"").append(h)
                .append("\" rx=\"").append(arc / 2).append("\" ")
                .append(paint(fill, stroke, width)).append("/>\n");
    }

    @Override
    public void oval(int x, int y, int w, int h, Color fill, Color stroke, float width) {
        body.append("<ellipse cx=\"").append(x + w / 2.0).append("\" cy=\"").append(y + h / 2.0)
                .append("\" rx=\"").append(w / 2.0).append("\" ry=\"").append(h / 2.0).append("\" ")
                .append(paint(fill, stroke, width)).append("/>\n");
    }

    @Override
    public void polygon(int[] xs, int[] ys, Color fill, Color stroke, float width) {
        body.append("<polygon points=\"");
        for (int i = 0; i < xs.length; i++) {
            if (i > 0) {
                body.append(' ');
            }
            body.append(xs[i]).append(',').append(ys[i]);
        }
        body.append("\" ").append(paint(fill, stroke, width)).append("/>\n");
    }

    @Override
    public void text(String text, int x, int baseline, Font font, Color color) {
        body.append("<text x=\"").append(x).append("\" y=\"").append(baseline)
                .append("\" font-family=\"").append(escape(font.getFamily())).append("\" font-size=\"")
                .append(font.getSize()).append("\" fill=\"").append(hex(color)).append("\"");
        if (font.isItalic()) {
            body.append(" font-style=\"italic\"");
        }
        body.append(">").append(escape(text)).append("</text>\n");
    }

    public String document(int width, int height, Color background, int translateX, int translateY) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height
                + "\" viewBox=\"0 0 " + width + " " + height + "\">\n"
                + "<rect width=\"100%\" height=\"100%\" fill=\"" + hex(background) + "\"/>\n"
                + "<g transform=\"translate(" + translateX + " " + translateY + ")\">\n"
                + body
                + "</g>\n</svg>\n";
    }

    private static String paint(Color fill, Color stroke, float width) {
        String fillText = fill == null ? "none" : hex(fill);
        if (stroke == null) {
            return "fill=\"" + fillText + "\" stroke=\"none\"";
        }
        return "fill=\"" + fillText + "\" stroke=\"" + hex(stroke) + "\" stroke-width=\"" + width + "\"";
    }

    private static String stroke(Color color, float width, boolean dashed) {
        String dash = dashed ? " stroke-dasharray=\"7 5\"" : "";
        return "stroke=\"" + hex(color) + "\" stroke-width=\"" + width + "\" fill=\"none\"" + dash;
    }

    static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
