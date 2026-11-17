package jetuml.view;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Set;

import jetuml.model.Edge;
import jetuml.model.Element;
import jetuml.model.Node;

/** Visão do diagrama: pede o layout e delega nós e arestas. */
public final class DiagramPainter {
    public void paint(Draw2D g, Layout layout, Theme theme, boolean grid, int canvasWidth, int canvasHeight,
            Set<Element> selected, Rectangle rubber, int[] band) {
        if (grid) {
            Color gridColor = theme.grid();
            for (int x = 0; x <= canvasWidth; x += jetuml.model.Geom.GRID) {
                g.line(x, 0, x, canvasHeight, gridColor, 1f, false);
            }
            for (int y = 0; y <= canvasHeight; y += jetuml.model.Geom.GRID) {
                g.line(0, y, canvasWidth, y, gridColor, 1f, false);
            }
        }
        for (Node node : layout.diagram().paintOrder()) {
            new NodePainting(g, layout, theme, selected.contains(node)).paint(node);
        }
        for (Edge edge : layout.diagram().edges()) {
            drawEdge(g, layout, theme, edge, selected.contains(edge));
        }
        if (rubber != null) {
            g.rect(rubber.x, rubber.y, rubber.width, rubber.height, null, theme.selection(), 1f);
        }
        if (band != null) {
            g.line(band[0], band[1], band[2], band[3], theme.selection(), 1.4f, false);
        }
    }

    private void drawEdge(Draw2D g, Layout layout, Theme theme, Edge edge, boolean selected) {
        List<Point> path = layout.path(edge);
        if (path.size() < 2) {
            return;
        }
        EdgeDecoration.of(edge).paint(new EdgeDecoration.Context(g, layout, theme, selected), edge, path);
    }
}
