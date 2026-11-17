package jetuml.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;

public final class SwingDraw implements Draw2D {
    private final Graphics2D graphics;

    public SwingDraw(Graphics2D graphics) {
        this.graphics = graphics;
    }

    @Override
    public void line(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed) {
        graphics.setColor(color);
        graphics.setStroke(stroke(width, dashed));
        graphics.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void polyline(int[] xs, int[] ys, Color color, float width, boolean dashed) {
        graphics.setColor(color);
        graphics.setStroke(stroke(width, dashed));
        graphics.drawPolyline(xs, ys, xs.length);
    }

    @Override
    public void rect(int x, int y, int w, int h, Color fill, Color stroke, float width) {
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fillRect(x, y, w, h);
        }
        if (stroke != null) {
            graphics.setColor(stroke);
            graphics.setStroke(stroke(width, false));
            graphics.drawRect(x, y, w, h);
        }
    }

    @Override
    public void roundRect(int x, int y, int w, int h, int arc, Color fill, Color stroke, float width) {
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fillRoundRect(x, y, w, h, arc, arc);
        }
        if (stroke != null) {
            graphics.setColor(stroke);
            graphics.setStroke(stroke(width, false));
            graphics.drawRoundRect(x, y, w, h, arc, arc);
        }
    }

    @Override
    public void oval(int x, int y, int w, int h, Color fill, Color stroke, float width) {
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fillOval(x, y, w, h);
        }
        if (stroke != null) {
            graphics.setColor(stroke);
            graphics.setStroke(stroke(width, false));
            graphics.drawOval(x, y, w, h);
        }
    }

    @Override
    public void polygon(int[] xs, int[] ys, Color fill, Color stroke, float width) {
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fillPolygon(xs, ys, xs.length);
        }
        if (stroke != null) {
            graphics.setColor(stroke);
            graphics.setStroke(stroke(width, false));
            graphics.drawPolygon(xs, ys, xs.length);
        }
    }

    @Override
    public void text(String text, int x, int baseline, Font font, Color color) {
        graphics.setFont(font);
        graphics.setColor(color);
        graphics.drawString(text, x, baseline);
    }

    private static Stroke stroke(float width, boolean dashed) {
        if (dashed) {
            return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {7f, 5f}, 0f);
        }
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
