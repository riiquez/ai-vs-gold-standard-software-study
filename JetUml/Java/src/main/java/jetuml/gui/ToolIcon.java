package jetuml.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

import jetuml.app.Tool;

/** Ícones desenhados em código para a barra não depender de arquivos externos. */
public final class ToolIcon implements Icon {
    private final Tool tool;
    private final Color ink;

    public ToolIcon(Tool tool, Color ink) {
        this.tool = tool;
        this.ink = ink;
    }

    @Override
    public int getIconWidth() {
        return 22;
    }

    @Override
    public int getIconHeight() {
        return 22;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.setColor(ink);
        g.setStroke(new BasicStroke(1.3f));
        switch (tool) {
            case SELECT -> {
                int[] xs = {2, 2, 8, 6, 10, 7, 11};
                int[] ys = {2, 16, 12, 12, 20, 12, 8};
                g.drawPolyline(new int[] {3, 3, 9, 7, 15}, new int[] {2, 18, 13, 13, 19}, 5);
                g.drawLine(7, 13, 12, 8);
            }
            case CLASS, INTERFACE -> {
                g.drawRect(3, 2, 16, 18);
                g.drawLine(3, 8, 19, 8);
                g.drawLine(3, 14, 19, 14);
            }
            case PACKAGE, PACKAGE_DESCRIPTION -> {
                g.drawRect(3, 7, 16, 12);
                g.drawRect(3, 4, 8, 4);
            }
            case NOTE -> {
                g.drawLine(4, 3, 13, 3);
                g.drawLine(13, 3, 18, 8);
                g.drawLine(18, 8, 18, 19);
                g.drawLine(18, 19, 4, 19);
                g.drawLine(4, 19, 4, 3);
                g.drawLine(13, 3, 13, 8);
                g.drawLine(13, 8, 18, 8);
            }
            case DEPENDENCY, NOTE_CONNECTOR, RETURN, EXTEND, INCLUDE -> {
                dashed(g);
                g.drawLine(3, 16, 16, 6);
                arrow(g, 16, 6, 3, 16);
            }
            case INHERITANCE, GENERALIZATION, REALIZATION -> {
                if (tool == Tool.REALIZATION) {
                    dashed(g);
                }
                g.drawLine(4, 18, 14, 6);
                g.drawPolygon(new int[] {14, 10, 18}, new int[] {4, 10, 10}, 3);
            }
            case ASSOCIATION, UC_ASSOCIATION, COLLABORATION -> g.drawLine(3, 18, 18, 4);
            case AGGREGATION -> {
                g.drawPolygon(new int[] {3, 7, 11, 7}, new int[] {12, 8, 12, 16}, 4);
                g.drawLine(11, 12, 19, 5);
            }
            case COMPOSITION -> {
                g.fillPolygon(new int[] {3, 7, 11, 7}, new int[] {12, 8, 12, 16}, 4);
                g.drawLine(11, 12, 19, 5);
            }
            case TO_CLIPBOARD -> {
                g.drawRoundRect(5, 4, 12, 15, 2, 2);
                g.drawRect(8, 2, 6, 3);
                g.drawLine(8, 10, 14, 10);
                g.drawLine(8, 13, 14, 13);
            }
            case IMPLICIT_PARAMETER -> {
                g.drawRect(4, 2, 14, 7);
                dashed(g);
                g.drawLine(11, 9, 11, 20);
            }
            case CALL -> {
                g.drawRect(3, 6, 5, 12);
                g.drawLine(8, 10, 16, 10);
                arrow(g, 16, 10, 8, 10);
                g.drawRect(16, 6, 4, 10);
            }
            case STATE -> g.drawRoundRect(3, 5, 16, 12, 8, 8);
            case INITIAL_STATE -> g.fillOval(6, 6, 10, 10);
            case FINAL_STATE -> {
                g.drawOval(4, 4, 14, 14);
                g.fillOval(8, 8, 6, 6);
            }
            case TRANSITION -> {
                g.drawLine(3, 16, 16, 6);
                arrow(g, 16, 6, 3, 16);
            }
            case OBJECT -> {
                g.drawRect(3, 4, 16, 14);
                g.drawLine(6, 11, 16, 11);
            }
            case FIELD -> {
                g.drawRect(3, 7, 16, 8);
                g.drawString("=", 9, 14);
            }
            case REFERENCE -> {
                g.drawRect(2, 8, 8, 6);
                g.drawLine(10, 11, 16, 11);
                arrow(g, 16, 11, 10, 11);
            }
            case ACTOR -> {
                g.drawOval(8, 2, 6, 6);
                g.drawLine(11, 8, 11, 14);
                g.drawLine(6, 11, 16, 11);
                g.drawLine(11, 14, 7, 20);
                g.drawLine(11, 14, 15, 20);
            }
            case USE_CASE -> g.drawOval(2, 5, 18, 12);
            default -> g.drawRect(4, 4, 14, 14);
        }
        g.dispose();
    }

    private static void arrow(Graphics2D g, int x, int y, int fromX, int fromY) {
        double dx = x - fromX;
        double dy = y - fromY;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return;
        }
        dx /= len;
        dy /= len;
        int x1 = (int) Math.round(x - dx * 6 - dy * 3);
        int y1 = (int) Math.round(y - dy * 6 + dx * 3);
        int x2 = (int) Math.round(x - dx * 6 + dy * 3);
        int y2 = (int) Math.round(y - dy * 6 - dx * 3);
        g.drawLine(x, y, x1, y1);
        g.drawLine(x, y, x2, y2);
    }

    private static void dashed(Graphics2D g) {
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {3f, 2f}, 0f));
    }
}
