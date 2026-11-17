package jetuml.view;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.NodeVisitor;
import jetuml.model.nodes.ActorNode;
import jetuml.model.nodes.CallNode;
import jetuml.model.nodes.ClassNode;
import jetuml.model.nodes.FieldNode;
import jetuml.model.nodes.FinalStateNode;
import jetuml.model.nodes.ImplicitParameterNode;
import jetuml.model.nodes.InitialStateNode;
import jetuml.model.nodes.InterfaceNode;
import jetuml.model.nodes.NoteNode;
import jetuml.model.nodes.ObjectNode;
import jetuml.model.nodes.PackageDescriptionNode;
import jetuml.model.nodes.PackageNode;
import jetuml.model.nodes.PointNode;
import jetuml.model.nodes.StateNode;
import jetuml.model.nodes.UseCaseNode;

/** Visão de cada subclasse de nó. O modelo só aceita a visita. */
final class NodePainting implements NodeVisitor<Void> {
    private final Draw2D g;
    private final Layout layout;
    private final Theme theme;
    private final boolean selected;

    NodePainting(Draw2D g, Layout layout, Theme theme, boolean selected) {
        this.g = g;
        this.layout = layout;
        this.theme = theme;
        this.selected = selected;
    }

    void paint(Node node) {
        node.accept(this);
    }

    @Override
    public Void visitClass(ClassNode node) {
        Rectangle box = layout.bounds(node);
        Color ink = theme.ink();
        Color stroke = stroke();
        float width = width();
        Measure measure = layout.measure();
        g.rect(box.x, box.y, box.width, box.height, theme.fill(), stroke, width);
        int line = measure.lineHeight();
        List<String> attributes = TextDraw.blank(measure.lines(node.get("attributes")));
        int nameBottom = box.y + line + 12;
        int attrBottom = nameBottom + attributes.size() * line + 10;
        g.line(box.x, nameBottom, box.x + box.width, nameBottom, stroke, 1f, false);
        g.line(box.x, attrBottom, box.x + box.width, attrBottom, stroke, 1f, false);
        TextDraw.centered(g, measure, node.get("name"), box.x, box.y + 6, box.width, ink);
        TextDraw.rows(g, measure, attributes, box.x + 8, nameBottom + 2, ink);
        TextDraw.rows(g, measure, TextDraw.blank(measure.lines(node.get("methods"))), box.x + 8, attrBottom + 2, ink);
        return null;
    }

    @Override
    public Void visitInterface(InterfaceNode node) {
        Rectangle box = layout.bounds(node);
        Color ink = theme.ink();
        Color stroke = stroke();
        Measure measure = layout.measure();
        g.rect(box.x, box.y, box.width, box.height, theme.fill(), stroke, width());
        int line = measure.lineHeight();
        int split = box.y + line * 2 + 12;
        g.line(box.x, split, box.x + box.width, split, stroke, 1f, false);
        TextDraw.centered(g, measure, "\u00ABinterface\u00BB", box.x, box.y + 4, box.width, ink);
        TextDraw.centered(g, measure, node.get("name"), box.x, box.y + line + 4, box.width, ink);
        TextDraw.rows(g, measure, TextDraw.blank(measure.lines(node.get("methods"))), box.x + 8, split + 2, ink);
        return null;
    }

    @Override
    public Void visitPackage(PackageNode node) {
        drawPackage(layout.bounds(node), node.get("name"), null);
        return null;
    }

    @Override
    public Void visitPackageDescription(PackageDescriptionNode node) {
        Measure measure = layout.measure();
        drawPackage(layout.bounds(node), node.get("name"), TextDraw.blank(measure.lines(node.get("content"))));
        return null;
    }

    @Override
    public Void visitNote(NoteNode node) {
        Rectangle box = layout.bounds(node);
        Color stroke = stroke();
        int fold = 14;
        int[] xs = {box.x, box.x + box.width - fold, box.x + box.width, box.x + box.width, box.x};
        int[] ys = {box.y, box.y, box.y + fold, box.y + box.height, box.y + box.height};
        g.polygon(xs, ys, theme.note(), stroke, width());
        g.line(box.x + box.width - fold, box.y, box.x + box.width - fold, box.y + fold, stroke, 1f, false);
        g.line(box.x + box.width - fold, box.y + fold, box.x + box.width, box.y + fold, stroke, 1f, false);
        TextDraw.rows(g, layout.measure(), TextDraw.blank(layout.measure().lines(node.get("text"))), box.x + 8, box.y + 6, theme.ink());
        return null;
    }

    @Override
    public Void visitPoint(PointNode node) {
        Rectangle box = layout.bounds(node);
        Color stroke = stroke();
        g.oval(box.x, box.y, box.width, box.height, stroke, stroke, 1f);
        return null;
    }

    @Override
    public Void visitState(StateNode node) {
        Rectangle box = layout.bounds(node);
        Measure measure = layout.measure();
        g.roundRect(box.x, box.y, box.width, box.height, 18, theme.fill(), stroke(), width());
        TextDraw.centered(g, measure, node.get("name"), box.x, box.y + (box.height - measure.lineHeight()) / 2, box.width, theme.ink());
        return null;
    }

    @Override
    public Void visitInitialState(InitialStateNode node) {
        Rectangle box = layout.bounds(node);
        g.oval(box.x, box.y, box.width, box.height, theme.ink(), theme.ink(), 1f);
        return null;
    }

    @Override
    public Void visitFinalState(FinalStateNode node) {
        Rectangle box = layout.bounds(node);
        g.oval(box.x, box.y, box.width, box.height, theme.fill(), theme.ink(), 2f);
        int inset = 7;
        g.oval(box.x + inset, box.y + inset, box.width - inset * 2, box.height - inset * 2, theme.ink(), theme.ink(), 1f);
        return null;
    }

    @Override
    public Void visitActor(ActorNode node) {
        drawActor(layout.bounds(node), node.get("name"));
        return null;
    }

    @Override
    public Void visitUseCase(UseCaseNode node) {
        Rectangle box = layout.bounds(node);
        Measure measure = layout.measure();
        g.oval(box.x, box.y, box.width, box.height, theme.fill(), stroke(), width());
        TextDraw.centered(g, measure, node.get("name"), box.x, box.y + (box.height - measure.lineHeight()) / 2, box.width, theme.ink());
        return null;
    }

    @Override
    public Void visitObject(ObjectNode node) {
        Rectangle box = layout.bounds(node);
        Color stroke = stroke();
        Measure measure = layout.measure();
        g.rect(box.x, box.y, box.width, box.height, theme.fill(), stroke, width());
        int row = measure.lineHeight() + 8;
        int header = row + 8;
        g.line(box.x, box.y + header, box.x + box.width, box.y + header, stroke, 1f, false);
        TextDraw.centered(g, measure, node.get("name"), box.x, box.y + 6, box.width, theme.ink(), true);
        int index = 0;
        for (Node child : layout.diagram().children(node)) {
            if (child.type() != NodeType.FIELD) {
                continue;
            }
            int top = box.y + header + index * row;
            if (index > 0) {
                g.line(box.x, top, box.x + box.width, top, theme.muted(), 1f, false);
            }
            TextDraw.rich(g, measure, Layout.fieldLabel(child), box.x + 8, top + 4 + measure.ascent(), theme.ink());
            index++;
        }
        return null;
    }

    @Override
    public Void visitField(FieldNode node) {
        return null;
    }

    @Override
    public Void visitImplicitParameter(ImplicitParameterNode node) {
        Rectangle head = layout.header(node);
        Measure measure = layout.measure();
        g.rect(head.x, head.y, head.width, head.height, theme.fill(), stroke(), width());
        TextDraw.centered(g, measure, node.get("name"), head.x, head.y + (head.height - measure.lineHeight()) / 2, head.width, theme.ink(), true);
        int lx = head.x + head.width / 2;
        g.line(lx, head.y + head.height, lx, layout.sequenceBottom(), theme.muted(), 1.2f, true);
        return null;
    }

    @Override
    public Void visitCall(CallNode node) {
        Rectangle box = layout.bounds(node);
        Color stroke = stroke();
        g.rect(box.x, box.y, box.width, box.height, theme.activation(), stroke, width());
        if (node.flag("openBottom")) {
            g.rect(box.x, box.y + box.height - 2, box.width, 4, theme.canvas(), null, 1f);
            g.line(box.x, box.y + box.height + 6, box.x, box.y + box.height + 16, stroke, 1.2f, true);
            g.line(box.x + box.width, box.y + box.height + 6, box.x + box.width, box.y + box.height + 16, stroke, 1.2f, true);
        }
        return null;
    }

    private Color stroke() {
        return selected ? theme.selection() : theme.ink();
    }

    private float width() {
        return selected ? 2f : 1.2f;
    }

    private void drawPackage(Rectangle box, String name, List<String> content) {
        Measure measure = layout.measure();
        Color stroke = stroke();
        int tabH = measure.lineHeight() + 10;
        int tabW = Math.min(box.width, Math.max(84, measure.width(name) + 18));
        g.rect(box.x, box.y + tabH, box.width, Math.max(8, box.height - tabH), theme.packageFill(), stroke, width());
        g.rect(box.x, box.y, tabW, tabH + 1, theme.packageFill(), stroke, width());
        TextDraw.centered(g, measure, name, box.x, box.y + 4, tabW, theme.ink());
        if (content != null) {
            TextDraw.rows(g, measure, content, box.x + 8, box.y + tabH + 4, theme.ink());
        }
    }

    private void drawActor(Rectangle box, String name) {
        Color stroke = stroke();
        float width = width();
        int cx = box.x + box.width / 2;
        int head = 22;
        g.oval(cx - head / 2, box.y, head, head, null, stroke, width);
        int neck = box.y + head;
        int hip = neck + 28;
        g.line(cx, neck, cx, hip, stroke, width, false);
        g.line(cx - 18, neck + 12, cx + 18, neck + 12, stroke, width, false);
        g.line(cx, hip, cx - 16, hip + 24, stroke, width, false);
        g.line(cx, hip, cx + 16, hip + 24, stroke, width, false);
        TextDraw.centered(g, layout.measure(), name, box.x, box.y + 78, box.width, theme.ink());
    }
}
