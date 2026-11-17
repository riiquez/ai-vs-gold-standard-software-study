package jetuml.view;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import jetuml.model.Edge;
import jetuml.model.EdgeKind;

/**
 * Decorador do desenho da aresta. Cada camada acrescenta linha, ponta ou rótulo
 * sem a aresta precisar mudar de classe quando o tipo visual muda.
 */
public interface EdgeDecoration {
    void paint(Context context, Edge edge, List<Point> path);

    record Context(Draw2D g, Layout layout, Theme theme, boolean selected) {
        Color color() {
            return selected ? theme.selection() : theme.ink();
        }

        float width() {
            return selected ? 2.1f : 1.3f;
        }
    }

    static EdgeDecoration of(Edge edge) {
        Marker end = Marker.NONE;
        Marker start = Marker.NONE;
        boolean dashed = false;
        int shortenEnd = 0;
        int shortenStart = 0;
        switch (edge.kind()) {
            case INHERITANCE, GENERALIZATION -> {
                end = Marker.TRIANGLE;
                shortenEnd = 14;
                if (bidirectional(edge)) {
                    start = Marker.TRIANGLE;
                    shortenStart = 14;
                }
            }
            case REALIZATION -> {
                dashed = true;
                end = Marker.TRIANGLE;
                shortenEnd = 14;
                if (bidirectional(edge)) {
                    start = Marker.TRIANGLE;
                    shortenStart = 14;
                }
            }
            case DEPENDENCY, INCLUDE, EXTEND, USE_DEPENDENCY -> {
                dashed = true;
                end = Marker.OPEN;
                shortenEnd = 11;
                if (bidirectional(edge)) {
                    start = Marker.OPEN;
                    shortenStart = 11;
                }
            }
            case ASSOCIATION, USE_ASSOCIATION, COLLABORATION -> {
                if (unidirectional(edge)) {
                    end = Marker.OPEN;
                    shortenEnd = 11;
                } else if (bidirectional(edge)) {
                    start = Marker.OPEN;
                    end = Marker.OPEN;
                    shortenStart = 11;
                    shortenEnd = 11;
                }
            }
            case AGGREGATION, COMPOSITION -> {
                start = edge.kind() == EdgeKind.COMPOSITION ? Marker.DIAMOND_FILLED : Marker.DIAMOND;
                shortenStart = 16;
                if (unidirectional(edge)) {
                    end = Marker.OPEN;
                    shortenEnd = 11;
                } else if (bidirectional(edge)) {
                    end = Marker.OPEN;
                    shortenEnd = 11;
                }
            }
            case NOTE -> dashed = true;
            case TRANSITION, REFERENCE -> {
                end = Marker.OPEN;
                shortenEnd = 11;
            }
            case CALL -> {
                end = Marker.FILLED;
                shortenEnd = 10;
            }
            case RETURN -> {
                dashed = true;
                end = Marker.OPEN;
                shortenEnd = 11;
            }
            default -> {
            }
        }
        EdgeDecoration line = new LineDecoration(dashed);
        EdgeDecoration marked = new MarkerDecoration(line, start, end, shortenStart, shortenEnd);
        return new LabelDecoration(marked);
    }

    final class LineDecoration implements EdgeDecoration {
        private final boolean dashed;

        LineDecoration(boolean dashed) {
            this.dashed = dashed;
        }

        @Override
        public void paint(Context context, Edge edge, List<Point> path) {
            int[] xs = new int[path.size()];
            int[] ys = new int[path.size()];
            for (int i = 0; i < path.size(); i++) {
                xs[i] = path.get(i).x;
                ys[i] = path.get(i).y;
            }
            context.g().polyline(xs, ys, context.color(), context.width(), dashed);
        }
    }

    final class MarkerDecoration implements EdgeDecoration {
        private final EdgeDecoration inner;
        private final Marker start;
        private final Marker end;
        private final int shortenStart;
        private final int shortenEnd;

        MarkerDecoration(EdgeDecoration inner, Marker start, Marker end, int shortenStart, int shortenEnd) {
            this.inner = inner;
            this.start = start;
            this.end = end;
            this.shortenStart = shortenStart;
            this.shortenEnd = shortenEnd;
        }

        @Override
        public void paint(Context context, Edge edge, List<Point> path) {
            inner.paint(context, edge, shorten(path, shortenStart, shortenEnd));
            drawMarker(context, path.get(0), path.get(1), start, true);
            drawMarker(context, path.get(path.size() - 1), path.get(path.size() - 2), end, false);
        }
    }

    final class LabelDecoration implements EdgeDecoration {
        private final EdgeDecoration inner;

        LabelDecoration(EdgeDecoration inner) {
            this.inner = inner;
        }

        @Override
        public void paint(Context context, Edge edge, List<Point> path) {
            inner.paint(context, edge, path);
            String middle = middleText(edge);
            if (!middle.isBlank()) {
                Point at = context.layout().labelPoint(edge);
                Measure measure = context.layout().measure();
                int textWidth = measure.width(middle);
                int top = edge.start() == edge.end() ? at.y + 4 : at.y - measure.lineHeight() - 1;
                TextDraw.rich(context.g(), measure, middle, at.x - textWidth / 2 + 10, top, context.color());
            }
            drawEndLabel(context, edge.get("startLabel"), path.get(0), path.get(1), true);
            drawEndLabel(context, edge.get("endLabel"), path.get(path.size() - 1), path.get(path.size() - 2), false);
        }
    }

    enum Marker { NONE, OPEN, FILLED, TRIANGLE, DIAMOND, DIAMOND_FILLED }

    private static boolean unidirectional(Edge edge) {
        return "Unidirectional".equals(edge.get("directionality"));
    }

    private static boolean bidirectional(Edge edge) {
        return "Bidirectional".equals(edge.get("directionality"));
    }

    private static String middleText(Edge edge) {
        return switch (edge.kind()) {
            case INCLUDE -> "\u00ABinclude\u00BB";
            case EXTEND -> "\u00ABextend\u00BB";
            default -> edge.get("middleLabel");
        };
    }

    private static void drawEndLabel(Context context, String text, Point tip, Point other, boolean start) {
        if (text == null || text.isBlank() || other == null) {
            return;
        }
        double dx = tip.x - other.x;
        double dy = tip.y - other.y;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return;
        }
        int x = (int) Math.round(tip.x - dx / len * 18 + (-dy / len) * 12);
        int y = (int) Math.round(tip.y - dy / len * 18 + (dx / len) * 12);
        TextDraw.centered(context.g(), context.layout().measure(), text, x - 40, y - context.layout().measure().ascent(),
                80, context.color());
    }

    private static void drawMarker(Context context, Point tip, Point from, Marker marker, boolean atStart) {
        if (marker == Marker.NONE || from == null) {
            return;
        }
        double dx = tip.x - from.x;
        double dy = tip.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return;
        }
        dx /= len;
        dy /= len;
        double px = -dy;
        double py = dx;
        Theme theme = context.theme();
        Draw2D g = context.g();
        if (marker == Marker.DIAMOND || marker == Marker.DIAMOND_FILLED) {
            int d = 14;
            int half = 6;
            double ox = atStart ? dx : -dx;
            double oy = atStart ? dy : -dy;
            int[] xs = {
                    tip.x,
                    (int) Math.round(tip.x + ox * d / 2 + px * half),
                    (int) Math.round(tip.x + ox * d),
                    (int) Math.round(tip.x + ox * d / 2 - px * half)
            };
            int[] ys = {
                    tip.y,
                    (int) Math.round(tip.y + oy * d / 2 + py * half),
                    (int) Math.round(tip.y + oy * d),
                    (int) Math.round(tip.y + oy * d / 2 - py * half)
            };
            Color fill = marker == Marker.DIAMOND_FILLED ? theme.ink() : theme.fill();
            g.polygon(xs, ys, fill, theme.ink(), 1.2f);
            return;
        }
        int length = marker == Marker.OPEN ? 11 : 14;
        int spread = marker == Marker.OPEN ? 5 : 6;
        int bx = (int) Math.round(tip.x - dx * length);
        int by = (int) Math.round(tip.y - dy * length);
        int x1 = (int) Math.round(bx + px * spread);
        int y1 = (int) Math.round(by + py * spread);
        int x2 = (int) Math.round(bx - px * spread);
        int y2 = (int) Math.round(by - py * spread);
        if (marker == Marker.OPEN) {
            g.line(tip.x, tip.y, x1, y1, theme.ink(), 1.3f, false);
            g.line(tip.x, tip.y, x2, y2, theme.ink(), 1.3f, false);
        } else {
            Color fill = marker == Marker.FILLED ? theme.ink() : theme.fill();
            g.polygon(new int[] {tip.x, x1, x2}, new int[] {tip.y, y1, y2}, fill, theme.ink(), 1.2f);
        }
    }

    private static List<Point> shorten(List<Point> path, int startTrim, int endTrim) {
        List<Point> copy = new ArrayList<>();
        for (Point point : path) {
            copy.add(new Point(point));
        }
        if (copy.size() >= 2 && startTrim > 0) {
            copy.set(0, moveToward(copy.get(0), copy.get(1), startTrim));
        }
        if (copy.size() >= 2 && endTrim > 0) {
            int last = copy.size() - 1;
            copy.set(last, moveToward(copy.get(last), copy.get(last - 1), endTrim));
        }
        return copy;
    }

    private static Point moveToward(Point from, Point to, int distance) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return new Point(from);
        }
        double step = Math.min(distance, len);
        return new Point((int) Math.round(from.x + dx / len * step), (int) Math.round(from.y + dy / len * step));
    }
}
