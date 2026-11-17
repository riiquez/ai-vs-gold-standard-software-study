package jetuml.view;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetuml.model.Diagram;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.rules.SequenceGestures;

/** Geometria calculada a partir dos nós. Mover um nó recalcula as arestas (RF17). */
public final class Layout {
    public enum Part { BODY, HEADER, LIFELINE }

    private final Diagram diagram;
    private final Measure measure;
    private final Map<Node, Rectangle> bounds = new HashMap<>();
    private final Map<Node, Rectangle> headers = new HashMap<>();
    private final Map<Edge, List<Point>> paths = new HashMap<>();
    private int sequenceBottom;

    private Layout(Diagram diagram, Measure measure) {
        this.diagram = diagram;
        this.measure = measure;
    }

    public static Layout of(Diagram diagram, Measure measure) {
        Layout layout = new Layout(diagram, measure);
        layout.compute();
        return layout;
    }

    public Diagram diagram() {
        return diagram;
    }

    public Measure measure() {
        return measure;
    }

    public Rectangle bounds(Node node) {
        return bounds.getOrDefault(node, new Rectangle(node.x(), node.y(), 40, 30));
    }

    public Rectangle header(Node node) {
        return headers.getOrDefault(node, bounds(node));
    }

    public List<Point> path(Edge edge) {
        return paths.getOrDefault(edge, List.of());
    }

    /** Ponto do rótulo central. Arestas paralelas recebem alturas diferentes. */
    public Point labelPoint(Edge edge) {
        List<Point> path = path(edge);
        if (path.size() < 2) {
            return new Point(0, 0);
        }
        double t = 0.5;
        if (edge.start() != edge.end()) {
            int total = parallelCount(edge);
            if (total > 1) {
                t = 0.42 + 0.28 * parallelIndex(edge) / (total - 1);
            }
        }
        int best = 0;
        double bestLength = -1;
        for (int i = 0; i + 1 < path.size(); i++) {
            double length = Math.hypot(path.get(i + 1).x - path.get(i).x, path.get(i + 1).y - path.get(i).y);
            if (length > bestLength) {
                bestLength = length;
                best = i;
            }
        }
        Point start = path.get(best);
        Point end = path.get(best + 1);
        if (start.y > end.y || (start.y == end.y && start.x > end.x)) {
            Point swap = start;
            start = end;
            end = swap;
        }
        return new Point((int) Math.round(start.x + (end.x - start.x) * t),
                (int) Math.round(start.y + (end.y - start.y) * t));
    }

    public int sequenceBottom() {
        return sequenceBottom;
    }

    public Rectangle contentBounds() {
        Rectangle content = null;
        for (Node node : diagram.nodes()) {
            content = union(content, bounds(node));
            if (node.type() == NodeType.IMPLICIT_PARAMETER) {
                Rectangle head = header(node);
                content = union(content, new Rectangle(head.x + head.width / 2, head.y, 1, sequenceBottom - head.y));
            }
        }
        for (List<Point> path : paths.values()) {
            for (Point point : path) {
                content = union(content, new Rectangle(point.x, point.y, 1, 1));
            }
        }
        if (content == null) {
            return new Rectangle(0, 0, 80, 80);
        }
        return content;
    }

    private void compute() {
        for (Node node : diagram.nodes()) {
            if (node.type() != NodeType.FIELD && node.type() != NodeType.CALL) {
                Rectangle box = intrinsic(node);
                bounds.put(node, box);
                if (node.type() == NodeType.IMPLICIT_PARAMETER) {
                    headers.put(node, box);
                }
            }
        }
        List<Node> packages = new ArrayList<>();
        for (Node node : diagram.nodes()) {
            if (node.type() == NodeType.PACKAGE) {
                packages.add(node);
            }
        }
        packages.sort((a, b) -> Integer.compare(depth(b), depth(a)));
        for (Node pkg : packages) {
            Rectangle box = bounds.get(pkg);
            for (Node child : diagram.children(pkg)) {
                Rectangle childBox = bounds.get(child);
                if (childBox != null) {
                    box = union(box, new Rectangle(childBox.x - 16, childBox.y - 12, childBox.width + 32, childBox.height + 28));
                }
            }
            bounds.put(pkg, box);
        }
        for (Node node : diagram.nodes()) {
            if (node.type() == NodeType.OBJECT) {
                layoutFields(node);
            }
        }
        layoutSequence();
        for (Edge edge : diagram.edges()) {
            paths.put(edge, route(edge));
        }
    }

    private Rectangle intrinsic(Node node) {
        int x = node.x();
        int y = node.y();
        int line = measure.lineHeight();
        return switch (node.type()) {
            case CLASS -> {
                List<String> attributes = orBlank(measure.lines(node.get("attributes")));
                List<String> methods = orBlank(measure.lines(node.get("methods")));
                int width = measure.width(node.get("name"));
                for (String row : attributes) {
                    width = Math.max(width, measure.width(row));
                }
                for (String row : methods) {
                    width = Math.max(width, measure.width(row));
                }
                width = Math.max(120, width + 20);
                int height = line + 12 + attributes.size() * line + 10 + methods.size() * line + 10;
                yield new Rectangle(x, y, width, height);
            }
            case INTERFACE -> {
                List<String> methods = orBlank(measure.lines(node.get("methods")));
                int width = Math.max(measure.width("\u00ABinterface\u00BB"), measure.width(node.get("name")));
                for (String row : methods) {
                    width = Math.max(width, measure.width(row));
                }
                width = Math.max(120, width + 20);
                int height = line * 2 + 12 + methods.size() * line + 10;
                yield new Rectangle(x, y, width, height);
            }
            case PACKAGE -> {
                int tab = Math.max(84, measure.width(node.get("name")) + 18);
                int width = Math.max(150, tab + 24);
                yield new Rectangle(x, y, width, line + 10 + 72);
            }
            case PACKAGE_DESCRIPTION -> {
                List<String> content = orBlank(measure.lines(node.get("content")));
                int width = measure.width(node.get("name"));
                for (String row : content) {
                    width = Math.max(width, measure.width(row));
                }
                width = Math.max(140, width + 20);
                yield new Rectangle(x, y, width, line + 12 + content.size() * line + 16);
            }
            case NOTE -> {
                List<String> content = orBlank(measure.lines(node.get("text")));
                int width = 80;
                for (String row : content) {
                    width = Math.max(width, measure.width(row) + 22);
                }
                yield new Rectangle(x, y, width, Math.max(52, content.size() * line + 18));
            }
            case POINT -> new Rectangle(x - 4, y - 4, 8, 8);
            case STATE -> {
                int width = Math.max(110, measure.width(node.get("name")) + 28);
                yield new Rectangle(x, y, width, Math.max(46, line + 20));
            }
            case INITIAL_STATE -> new Rectangle(x, y, 28, 28);
            case FINAL_STATE -> new Rectangle(x, y, 32, 32);
            case ACTOR -> new Rectangle(x, y, Math.max(72, measure.width(node.get("name")) + 12), 78 + line);
            case USE_CASE -> new Rectangle(x, y, Math.max(130, measure.width(node.get("name")) + 52), Math.max(56, line + 30));
            case OBJECT -> objectBox(node);
            case IMPLICIT_PARAMETER -> new Rectangle(x, y, Math.max(96, measure.width(node.get("name")) + 24), SequenceGestures.headerHeight(node));
            default -> new Rectangle(x, y, 80, 40);
        };
    }

    private Rectangle objectBox(Node node) {
        int line = measure.lineHeight();
        int row = line + 8;
        int width = Math.max(120, measure.width(node.get("name")) + 20);
        for (Node field : diagram.children(node)) {
            if (field.type() == NodeType.FIELD) {
                width = Math.max(width, measure.width(fieldLabel(field)) + 20);
            }
        }
        int fields = 0;
        for (Node child : diagram.children(node)) {
            if (child.type() == NodeType.FIELD) {
                fields++;
            }
        }
        int height = row + 8 + Math.max(fields, 0) * row + (fields == 0 ? 6 : 4);
        return new Rectangle(node.x(), node.y(), width, height);
    }

    private void layoutFields(Node object) {
        Rectangle box = objectBox(object);
        bounds.put(object, box);
        int line = measure.lineHeight();
        int row = line + 8;
        int header = row + 8;
        int index = 0;
        for (Node child : diagram.children(object)) {
            if (child.type() != NodeType.FIELD) {
                continue;
            }
            bounds.put(child, new Rectangle(box.x, box.y + header + index * row, box.width, row));
            index++;
        }
    }

    private void layoutSequence() {
        sequenceBottom = 0;
        for (Node node : diagram.nodes()) {
            if (node.type() != NodeType.IMPLICIT_PARAMETER) {
                continue;
            }
            Rectangle head = bounds.get(node);
            if (head == null) {
                continue;
            }
            headers.put(node, head);
            sequenceBottom = Math.max(sequenceBottom, head.y + head.height + 120);
            for (Node call : SequenceGestures.callsOn(diagram, node)) {
                int nest = SequenceGestures.nestLevel(diagram, call);
                int width = 16;
                int cx = head.x + head.width / 2 - width / 2 + nest * 14;
                int height = SequenceGestures.height(diagram, call);
                Rectangle callBox = new Rectangle(cx, call.y(), width, height);
                bounds.put(call, callBox);
                sequenceBottom = Math.max(sequenceBottom, callBox.y + callBox.height + 36);
            }
        }
    }

    private List<Point> route(Edge edge) {
        if (edge.start() == null || edge.end() == null) {
            return List.of();
        }
        if (edge.kind() == EdgeKind.CALL || edge.kind() == EdgeKind.RETURN) {
            return routeMessage(edge);
        }
        if (edge.kind() == EdgeKind.REFERENCE && edge.start().type() == NodeType.FIELD) {
            Rectangle field = bounds(edge.start());
            Point from = new Point(field.x + field.width - 4, field.y + field.height / 2);
            Point to = borderToward(connectBox(edge.end()), from);
            return List.of(from, to);
        }
        if (edge.start() == edge.end()) {
            return selfLoop(bounds(edge.start()), selfIndex(edge));
        }
        Rectangle a = connectBox(edge.start());
        Rectangle b = connectBox(edge.end());
        int index = parallelIndex(edge);
        int total = parallelCount(edge);
        return straight(a, b, index, total);
    }

    private List<Point> routeMessage(Edge edge) {
        Rectangle from = bounds(edge.start());
        Rectangle to = bounds(edge.end());
        if (edge.isConstructor() && edge.end().parent() != null) {
            Rectangle head = header(edge.end().parent());
            Point target = new Point(head.x + (from.x < head.x ? 0 : head.width), head.y + head.height / 2);
            Point source = new Point(from.x + (from.x < head.x ? from.width : 0), target.y);
            return List.of(source, target);
        }
        if (edge.start().parent() != null && edge.start().parent() == edge.end().parent()) {
            if (edge.kind() == EdgeKind.RETURN) {
                int y = from.y + from.height - 10;
                int left = Math.min(from.x, to.x) - 18;
                return List.of(new Point(from.x, y), new Point(left, y), new Point(left, to.y + 12), new Point(to.x, to.y + 12));
            }
            int y = to.y + 10;
            int right = Math.max(from.x + from.width, to.x + to.width) + 18;
            return List.of(new Point(from.x + from.width, y), new Point(right, y), new Point(to.x + to.width, y));
        }
        int y = edge.kind() == EdgeKind.RETURN ? from.y + from.height - 10 : to.y + 12;
        boolean left = from.x > to.x;
        Point source = new Point(left ? from.x : from.x + from.width, y);
        Point target = new Point(left ? to.x + to.width : to.x, y);
        return List.of(source, target);
    }

    private static List<Point> selfLoop(Rectangle box, int index) {
        int rise = 28 + index * 18;
        int x1 = box.x + Math.max(12, box.width / 3);
        int x2 = box.x + Math.max(28, box.width * 2 / 3);
        return List.of(new Point(x1, box.y), new Point(x1, box.y - rise), new Point(x2, box.y - rise), new Point(x2, box.y));
    }

    private static List<Point> straight(Rectangle a, Rectangle b, int index, int total) {
        double ax = a.getCenterX();
        double ay = a.getCenterY();
        double bx = b.getCenterX();
        double by = b.getCenterY();
        double dx = bx - ax;
        double dy = by - ay;
        if (ay > by || (ay == by && ax > bx)) {
            dx = -dx;
            dy = -dy;
        }
        double len = Math.hypot(dx, dy);
        double off = (index - (total - 1) / 2.0) * 26;
        double px = 0;
        double py = 0;
        if (len > 1) {
            px = -dy / len * off;
            py = dx / len * off;
        }
        Point start = borderExit(a, ax + px, ay + py, bx + px, by + py);
        Point end = borderExit(b, bx + px, by + py, ax + px, ay + py);
        return List.of(start, end);
    }

    static Point borderExit(Rectangle rect, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double best = Double.MAX_VALUE;
        Point found = new Point((int) Math.round(x1), (int) Math.round(y1));
        if (Math.abs(dx) > 0.001) {
            best = consider(rect, x1, y1, dx, dy, (rect.x - x1) / dx, best, found, true);
            best = consider(rect, x1, y1, dx, dy, (rect.x + rect.width - x1) / dx, best, found, true);
        }
        if (Math.abs(dy) > 0.001) {
            best = consider(rect, x1, y1, dx, dy, (rect.y - y1) / dy, best, found, false);
            consider(rect, x1, y1, dx, dy, (rect.y + rect.height - y1) / dy, best, found, false);
        }
        return found;
    }

    private static double consider(Rectangle rect, double x1, double y1, double dx, double dy, double t, double best, Point found, boolean verticalSide) {
        if (t <= 0.01 || t >= best) {
            return best;
        }
        double x = x1 + t * dx;
        double y = y1 + t * dy;
        boolean inside = verticalSide
                ? y >= rect.y - 0.5 && y <= rect.y + rect.height + 0.5
                : x >= rect.x - 0.5 && x <= rect.x + rect.width + 0.5;
        if (!inside) {
            return best;
        }
        found.x = (int) Math.round(x);
        found.y = (int) Math.round(y);
        return t;
    }

    private Point borderToward(Rectangle rect, Point from) {
        return borderExit(rect, rect.getCenterX(), rect.getCenterY(), from.x, from.y);
    }

    private Rectangle connectBox(Node node) {
        if (node.type() == NodeType.IMPLICIT_PARAMETER) {
            return header(node);
        }
        return bounds(node);
    }

    private int selfIndex(Edge edge) {
        int index = 0;
        for (Edge other : diagram.edges()) {
            if (other == edge) {
                return index;
            }
            if (other.start() == edge.start() && other.end() == edge.end()) {
                index++;
            }
        }
        return index;
    }

    private int parallelIndex(Edge edge) {
        int index = 0;
        for (Edge other : diagram.edges()) {
            if (other == edge) {
                return index;
            }
            if (sameEnds(edge, other)) {
                index++;
            }
        }
        return index;
    }

    private int parallelCount(Edge edge) {
        int count = 0;
        for (Edge other : diagram.edges()) {
            if (sameEnds(edge, other)) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static boolean sameEnds(Edge a, Edge b) {
        if (a.start() == a.end() || b.start() == b.end()) {
            return false;
        }
        return (a.start() == b.start() && a.end() == b.end()) || (a.start() == b.end() && a.end() == b.start());
    }

    public ElementHit hit(int x, int y) {
        List<Node> order = diagram.paintOrder();
        for (int i = order.size() - 1; i >= 0; i--) {
            Node node = order.get(i);
            if (contains(node, x, y)) {
                Part part = partOf(node, x, y);
                return new ElementHit(node, part);
            }
        }
        Edge closest = null;
        double best = 7;
        for (Edge edge : diagram.edges()) {
            double distance = distanceTo(paths.get(edge), x, y);
            if (distance < best) {
                best = distance;
                closest = edge;
            }
        }
        return closest == null ? null : new ElementHit(closest, Part.BODY);
    }

    public boolean regionIntersects(jetuml.model.Element element, Rectangle region) {
        if (element instanceof Node node) {
            if (node.type() == NodeType.IMPLICIT_PARAMETER) {
                if (header(node).intersects(region)) {
                    return true;
                }
                Rectangle head = header(node);
                int lx = head.x + head.width / 2;
                return segmentIntersects(lx, head.y + head.height, lx, sequenceBottom, region);
            }
            return bounds(node).intersects(region);
        }
        if (element instanceof Edge edge) {
            List<Point> path = paths.get(edge);
            if (path == null) {
                return false;
            }
            for (int i = 0; i + 1 < path.size(); i++) {
                Point a = path.get(i);
                Point b = path.get(i + 1);
                if (segmentIntersects(a.x, a.y, b.x, b.y, region)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Node topPackageAt(int x, int y, Node ignoring) {
        List<Node> order = diagram.paintOrder();
        for (int i = order.size() - 1; i >= 0; i--) {
            Node node = order.get(i);
            if (node.type() != NodeType.PACKAGE || node == ignoring || diagram.isAncestor(ignoring, node)) {
                continue;
            }
            if (bounds(node).contains(x, y)) {
                return node;
            }
        }
        return null;
    }

    public Node objectAt(int x, int y) {
        List<Node> order = diagram.paintOrder();
        Node found = null;
        for (Node node : order) {
            if (node.type() == NodeType.OBJECT && bounds(node).contains(x, y)) {
                found = node;
            }
        }
        return found;
    }

    private boolean contains(Node node, int x, int y) {
        if (node.type() == NodeType.USE_CASE) {
            Rectangle box = bounds(node);
            double nx = (x - box.getCenterX()) / (box.width / 2.0);
            double ny = (y - box.getCenterY()) / (box.height / 2.0);
            return nx * nx + ny * ny <= 1;
        }
        if (node.type() == NodeType.INITIAL_STATE || node.type() == NodeType.FINAL_STATE) {
            Rectangle box = bounds(node);
            double nx = (x - box.getCenterX()) / (box.width / 2.0);
            double ny = (y - box.getCenterY()) / (box.height / 2.0);
            return nx * nx + ny * ny <= 1;
        }
        if (node.type() == NodeType.IMPLICIT_PARAMETER) {
            if (header(node).contains(x, y)) {
                return true;
            }
            Rectangle head = header(node);
            int lx = head.x + head.width / 2;
            return y >= head.y + head.height && y <= sequenceBottom && Math.abs(x - lx) <= 8;
        }
        return bounds(node).contains(x, y);
    }

    private Part partOf(Node node, int x, int y) {
        if (node.type() == NodeType.IMPLICIT_PARAMETER && !header(node).contains(x, y)) {
            return Part.LIFELINE;
        }
        if (node.type() == NodeType.IMPLICIT_PARAMETER) {
            return Part.HEADER;
        }
        return Part.BODY;
    }

    private static double distanceTo(List<Point> path, int x, int y) {
        if (path == null || path.size() < 2) {
            return Double.MAX_VALUE;
        }
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < path.size(); i++) {
            best = Math.min(best, distanceToSegment(path.get(i), path.get(i + 1), x, y));
        }
        return best;
    }

    private static double distanceToSegment(Point a, Point b, int x, int y) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len = dx * dx + dy * dy;
        double t = len == 0 ? 0 : ((x - a.x) * dx + (y - a.y) * dy) / len;
        t = Math.max(0, Math.min(1, t));
        double px = a.x + t * dx;
        double py = a.y + t * dy;
        return Math.hypot(x - px, y - py);
    }

    private static boolean segmentIntersects(int x1, int y1, int x2, int y2, Rectangle rect) {
        if (rect.contains(x1, y1) || rect.contains(x2, y2)) {
            return true;
        }
        return linesCross(x1, y1, x2, y2, rect.x, rect.y, rect.x + rect.width, rect.y)
                || linesCross(x1, y1, x2, y2, rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height)
                || linesCross(x1, y1, x2, y2, rect.x, rect.y, rect.x, rect.y + rect.height)
                || linesCross(x1, y1, x2, y2, rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height);
    }

    private static boolean linesCross(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy) {
        double d1 = cross(cx, cy, dx, dy, ax, ay);
        double d2 = cross(cx, cy, dx, dy, bx, by);
        double d3 = cross(ax, ay, bx, by, cx, cy);
        double d4 = cross(ax, ay, bx, by, dx, dy);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(int ax, int ay, int bx, int by, int cx, int cy) {
        return (bx - ax) * (double) (cy - ay) - (by - ay) * (double) (cx - ax);
    }

    private int depth(Node node) {
        int depth = 0;
        for (Node current = node.parent(); current != null; current = current.parent()) {
            depth++;
        }
        return depth;
    }

    private static List<String> orBlank(List<String> lines) {
        return lines.isEmpty() ? List.of("") : lines;
    }

    public static String fieldLabel(Node field) {
        String value = field.get("value");
        return field.get("name") + (value.isBlank() ? "" : " = " + value);
    }

    private static Rectangle union(Rectangle a, Rectangle b) {
        if (a == null) {
            return new Rectangle(b);
        }
        Rectangle copy = new Rectangle(a);
        copy.add(b);
        return copy;
    }

    public record ElementHit(jetuml.model.Element element, Part part) {
    }
}
