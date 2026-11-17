package jetuml;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import jetuml.app.ElementClipboard;
import jetuml.app.Tool;
import jetuml.cmd.Commands;
import jetuml.cmd.UndoStack;
import jetuml.model.Diagram;
import jetuml.model.DiagramType;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Geom;
import jetuml.model.Node;
import jetuml.model.NodeType;
import jetuml.model.nodes.ClassNode;
import jetuml.model.nodes.FieldNode;
import jetuml.persist.DiagramCodec;
import jetuml.rules.Rules;
import jetuml.rules.SequenceGestures;
import jetuml.view.Exporter;
import jetuml.view.Layout;
import jetuml.view.Measure;
import jetuml.view.RichText;

/** Verificação automática das regras, do JSON e do desenho. */
public final class Check {
    private static int failures;

    private Check() {
    }

    public static void main(String[] args) throws Exception {
        tools();
        richText();
        classRules();
        stateRules();
        objectRules();
        useCaseRules();
        noteRules();
        sequenceRules();
        clipboard();
        persistence();
        undo();
        geometry();
        structure();
        renderAll();
        if (failures > 0) {
            System.out.println(failures + " verification(s) failed.");
            System.exit(1);
        }
        System.out.println("JetUML verification passed.");
    }

    private static void tools() {
        expect(List.of("Select", "Class", "Interface", "Package", "Package Description", "Note", "Dependency",
                "Inheritance", "Realization", "Association", "Aggregation", "Composition", "Note Connector", "To Clipboard"),
                Tool.forDiagram(DiagramType.CLASS), "class tools");
        expect(List.of("Select", "Implicit Parameter", "Note", "Call", "Return", "Note Connector", "To Clipboard"),
                Tool.forDiagram(DiagramType.SEQUENCE), "sequence tools");
        expect(List.of("Select", "State", "Initial State", "Final State", "Note", "Transition", "Note Connector", "To Clipboard"),
                Tool.forDiagram(DiagramType.STATE), "state tools");
        expect(List.of("Select", "Object", "Field", "Note", "Reference", "Collaboration", "Note Connector", "To Clipboard"),
                Tool.forDiagram(DiagramType.OBJECT), "object tools");
        expect(List.of("Select", "Actor", "Use Case", "Note", "Association", "Include", "Extend", "Generalization",
                "Note Connector", "To Clipboard"), Tool.forDiagram(DiagramType.USE_CASE), "use case tools");
        check("1".equals(Tool.shortcut(0)) && "9".equals(Tool.shortcut(8)) && "0".equals(Tool.shortcut(9))
                && "A".equals(Tool.shortcut(10)) && "D".equals(Tool.shortcut(13)), "shortcuts");
        check(Geom.clampDiagram(10) == 250 && Geom.clampDiagram(9000) == 4000, "diagram size clamp");
    }

    private static void expect(List<String> labels, Tool[] tools, String message) {
        check(labels.size() == tools.length, message + " count");
        for (int i = 0; i < labels.size() && i < tools.length; i++) {
            check(labels.get(i).equals(tools[i].label()), message + " [" + i + "]");
        }
    }

    private static void richText() {
        var italic = RichText.parse("/Animal/");
        check(italic.size() == 1 && italic.get(0).italic() && !italic.get(0).underline() && italic.get(0).text().equals("Animal"),
                "italic markup");
        var under = RichText.parse("_id_");
        check(under.size() == 1 && under.get(0).underline() && !under.get(0).italic(), "underline markup");
        var both = RichText.parse("/_save_/");
        check(both.size() == 1 && both.get(0).italic() && both.get(0).underline() && both.get(0).text().equals("save"),
                "nested markup");
        var plain = RichText.parse("a/b");
        check(plain.size() == 1 && plain.get(0).text().equals("a/b") && !plain.get(0).italic(), "unclosed slash stays literal");
    }

    private static void classRules() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        Node a = node(diagram, NodeType.CLASS, 40, 40);
        Node b = node(diagram, NodeType.CLASS, 240, 40);
        check(Rules.canCreate(diagram, EdgeKind.INHERITANCE, a, a).kind() == jetuml.rules.Check.Kind.NOTIFY, "self inheritance");
        check(Rules.canCreate(diagram, EdgeKind.DEPENDENCY, a, a).kind() == jetuml.rules.Check.Kind.NOTIFY, "self dependency");
        check(Rules.canCreate(diagram, EdgeKind.ASSOCIATION, a, a).ok(), "self association");
        add(diagram, EdgeKind.ASSOCIATION, a, a);
        check(Rules.canCreate(diagram, EdgeKind.ASSOCIATION, a, a).kind() == jetuml.rules.Check.Kind.NOTIFY, "duplicate self association");
        check(Rules.canCreate(diagram, EdgeKind.INHERITANCE, a, b).ok(), "inheritance");
        add(diagram, EdgeKind.INHERITANCE, a, b);
        check(Rules.canCreate(diagram, EdgeKind.DEPENDENCY, b, a).kind() == jetuml.rules.Check.Kind.NOTIFY, "direct cycle");
        check(Rules.canCreate(diagram, EdgeKind.INHERITANCE, a, b).kind() == jetuml.rules.Check.Kind.NOTIFY, "duplicate inheritance");
        Node c = node(diagram, NodeType.CLASS, 40, 200);
        Node d = node(diagram, NodeType.CLASS, 240, 200);
        add(diagram, EdgeKind.AGGREGATION, c, d);
        check(Rules.canCreate(diagram, EdgeKind.ASSOCIATION, d, c).kind() == jetuml.rules.Check.Kind.NOTIFY, "association plus aggregation");
        check(Rules.canCreate(diagram, EdgeKind.COMPOSITION, c, d).kind() == jetuml.rules.Check.Kind.NOTIFY, "aggregation family duplicate");
    }

    private static void stateRules() {
        Diagram diagram = new Diagram(DiagramType.STATE);
        Node initial = node(diagram, NodeType.INITIAL_STATE, 40, 40);
        Node state = node(diagram, NodeType.STATE, 160, 40);
        Node end = node(diagram, NodeType.FINAL_STATE, 320, 40);
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, end, state).kind() == jetuml.rules.Check.Kind.NOTIFY, "leave final");
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, state, initial).kind() == jetuml.rules.Check.Kind.NOTIFY, "enter initial");
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, initial, state).ok(), "initial transition");
        add(diagram, EdgeKind.TRANSITION, state, end);
        add(diagram, EdgeKind.TRANSITION, state, end);
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, state, end).kind() == jetuml.rules.Check.Kind.NOTIFY, "third transition");
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, end, state).kind() == jetuml.rules.Check.Kind.NOTIFY, "reverse still blocked by final");
        add(diagram, EdgeKind.TRANSITION, state, state);
        add(diagram, EdgeKind.TRANSITION, state, state);
        check(Rules.canCreate(diagram, EdgeKind.TRANSITION, state, state).kind() == jetuml.rules.Check.Kind.NOTIFY, "third self transition");
    }

    private static void objectRules() {
        Diagram diagram = new Diagram(DiagramType.OBJECT);
        Node left = node(diagram, NodeType.OBJECT, 40, 40);
        Node right = node(diagram, NodeType.OBJECT, 240, 40);
        Node field = node(diagram, NodeType.FIELD, 40, 80);
        field.setParent(left);
        check(Rules.canCreate(diagram, EdgeKind.COLLABORATION, left, left).kind() == jetuml.rules.Check.Kind.NOTIFY, "self collaboration");
        check(Rules.canCreate(diagram, EdgeKind.COLLABORATION, left, right).ok(), "collaboration");
        add(diagram, EdgeKind.COLLABORATION, left, right);
        check(Rules.canCreate(diagram, EdgeKind.COLLABORATION, right, left).kind() == jetuml.rules.Check.Kind.NOTIFY, "collaboration cycle");
        check(Rules.canCreate(diagram, EdgeKind.REFERENCE, field, right).ok(), "reference");
        check(Rules.canCreate(diagram, EdgeKind.REFERENCE, left, right).kind() == jetuml.rules.Check.Kind.NOTIFY, "reference must start at a field");
    }

    private static void useCaseRules() {
        Diagram diagram = new Diagram(DiagramType.USE_CASE);
        Node actor = node(diagram, NodeType.ACTOR, 40, 40);
        Node use = node(diagram, NodeType.USE_CASE, 220, 40);
        Node other = node(diagram, NodeType.USE_CASE, 220, 180);
        check(Rules.canCreate(diagram, EdgeKind.USE_ASSOCIATION, actor, actor).kind() == jetuml.rules.Check.Kind.NOTIFY, "self association actor");
        check(Rules.canCreate(diagram, EdgeKind.GENERALIZATION, use, use).kind() == jetuml.rules.Check.Kind.NOTIFY, "self generalization");
        check(Rules.canCreate(diagram, EdgeKind.INCLUDE, actor, use).kind() == jetuml.rules.Check.Kind.NOTIFY, "include from actor");
        check(Rules.canCreate(diagram, EdgeKind.INCLUDE, use, other).ok(), "include");
        check(Rules.canCreate(diagram, EdgeKind.GENERALIZATION, actor, use).kind() == jetuml.rules.Check.Kind.NOTIFY, "actor generalizes use case");
        check(Rules.canCreate(diagram, EdgeKind.USE_ASSOCIATION, actor, use).ok(), "actor association");
    }

    private static void noteRules() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        Node note = node(diagram, NodeType.NOTE, 40, 40);
        Node type = node(diagram, NodeType.CLASS, 200, 40);
        Node point = node(diagram, NodeType.POINT, 80, 120);
        check(Rules.canCreate(diagram, EdgeKind.NOTE, type, note).ok(), "node to note");
        check(Rules.canCreate(diagram, EdgeKind.NOTE, note, point).ok(), "note to anchor");
        check(Rules.canCreate(diagram, EdgeKind.NOTE, type, type).kind() == jetuml.rules.Check.Kind.NOTIFY, "note without a note");
    }

    private static void sequenceRules() {
        Diagram diagram = new Diagram(DiagramType.SEQUENCE);
        Node a = node(diagram, NodeType.IMPLICIT_PARAMETER, 80, 40);
        Node b = node(diagram, NodeType.IMPLICIT_PARAMETER, 280, 40);
        Node c = node(diagram, NodeType.IMPLICIT_PARAMETER, 480, 40);
        SequenceGestures.Result first = SequenceGestures.create(diagram, a, false, b, false, 140);
        check(first.check().ok(), "found message");
        first.command().execute();
        SequenceGestures.Result secondRoot = SequenceGestures.create(diagram, b, false, c, false, 220);
        check(secondRoot.check().kind() == jetuml.rules.Check.Kind.NOTIFY, "second root");
        check(diagram.rootActivations() == 1, "one root");
        Node caller = diagram.callerOf(first.focus());
        SequenceGestures.Result self = SequenceGestures.create(diagram, caller, false, caller, false, 186);
        check(self.check().ok(), "self call " + self.check().kind() + " " + self.check().message());
        if (self.command() != null) {
            self.command().execute();
        }
        SequenceGestures.Result back = SequenceGestures.createReturn(diagram, first.focus(), diagram.callerOf(first.focus()));
        check(back.check().ok(), "matching return");
        SequenceGestures.Result jump = SequenceGestures.createReturn(diagram, caller, caller);
        check(jump.check().kind() == jetuml.rules.Check.Kind.SILENT, "return jump is silent");
        SequenceGestures.Result structural = SequenceGestures.create(diagram, a, false, note(diagram), false, 160);
        check(structural.check().kind() == jetuml.rules.Check.Kind.NOTIFY, "call to a note");
        int before = diagram.edges().size();
        SequenceGestures.Result outside = SequenceGestures.create(diagram, caller, false, c, false, 40);
        check(outside.check().kind() == jetuml.rules.Check.Kind.SILENT && diagram.edges().size() == before, "control-flow jump adds nothing");
    }

    private static Node note(Diagram diagram) {
        return node(diagram, NodeType.NOTE, 20, 200);
    }

    private static void clipboard() {
        Diagram source = new Diagram(DiagramType.CLASS);
        Node type = node(source, NodeType.CLASS, 30, 30);
        type.set("name", "Order");
        Node note = node(source, NodeType.NOTE, 180, 30);
        ElementClipboard.copy(source, List.of(type, note));
        Diagram sequence = new Diagram(DiagramType.SEQUENCE);
        check(ElementClipboard.paste(sequence).check().kind() == jetuml.rules.Check.Kind.NOTIFY, "paste into sequence");
        Diagram useCase = new Diagram(DiagramType.USE_CASE);
        check(ElementClipboard.paste(useCase).check().kind() == jetuml.rules.Check.Kind.NOTIFY, "paste incompatible");
        Diagram target = new Diagram(DiagramType.CLASS);
        ElementClipboard.Paste pasted = ElementClipboard.paste(target);
        check(pasted.check().ok(), "paste compatible");
        pasted.command().execute();
        check(target.nodes().size() == 2, "pasted nodes");
        check(target.nodes().get(0).get("name").equals("Order") || target.nodes().get(1).get("name").equals("Order"), "pasted name");

        Diagram objects = new Diagram(DiagramType.OBJECT);
        Node field = node(objects, NodeType.FIELD, 10, 10);
        ElementClipboard.copy(objects, List.of(field));
        check(ElementClipboard.paste(objects).check().message().contains("field"), "orphan field paste");
    }

    private static void persistence() throws Exception {
        Diagram diagram = sampleClass();
        String json = DiagramCodec.encode(diagram);
        check(json.contains("\"format\": \"jetuml-1\""), "format marker");
        check(json.contains("\"diagramType\": \"Class\""), "diagram type");
        Diagram restored = DiagramCodec.decode(json);
        check(restored.nodes().size() == diagram.nodes().size(), "node count");
        check(restored.edges().size() == diagram.edges().size(), "edge count");
        Node klass = restored.nodes().stream().filter(node -> node.type() == NodeType.CLASS).findFirst().orElseThrow();
        check(klass.get("name").contains("Customer") && klass.get("methods").contains("/total/"), "text survived");
        check(klass.parent() != null && klass.parent().type() == NodeType.PACKAGE, "package parent");
        boolean failed = false;
        try {
            DiagramCodec.decode("{\"diagramType\":\"Object\",\"nodes\":[{\"id\":\"n1\",\"type\":\"Field\",\"x\":1,\"y\":1}],\"edges\":[]}");
        } catch (IllegalArgumentException exception) {
            failed = exception.getMessage().contains("field");
        }
        check(failed, "reject loose field");
    }

    private static void structure() throws Exception {
        Node node = Node.create("n1", NodeType.CLASS, 0, 0);
        check(node instanceof ClassNode, "class node is its own type");
        check(Node.create("n2", NodeType.FIELD, 0, 0) instanceof FieldNode, "field node is its own type");
        check(!Node.create("n3", NodeType.FIELD, 0, 0).movable(), "field is not movable");
        check(!Node.create("n4", NodeType.CALL, 0, 0).followsPackageShift(), "activation ignores shift");
        check(node.get("name").equals("Class"), "class default name");
        forbid("src/main/java/jetuml/model", "javax.swing", "jetuml.view", "jetuml.gui", "jetuml.controller", "jetuml.persist");
        forbid("src/main/java/jetuml/rules", "javax.swing", "jetuml.view", "jetuml.gui", "jetuml.controller");
        forbid("src/main/java/jetuml/persist", "javax.swing", "jetuml.view", "jetuml.gui", "jetuml.controller");
        forbid("src/main/java/jetuml/view", "javax.swing", "jetuml.gui", "jetuml.controller");
        forbid("src/main/java/jetuml/controller", "javax.swing", "jetuml.gui");
    }

    private static void forbid(String root, String... banned) throws Exception {
        Path dir = Path.of(root);
        check(Files.isDirectory(dir), "layer folder " + root);
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                for (String token : banned) {
                    if (text.contains(token)) {
                        check(false, file.getFileName() + " must not depend on " + token);
                    }
                }
            }
        }
    }

    private static void undo() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        UndoStack stack = new UndoStack();
        Node node = Node.create(diagram.nextNodeId(), NodeType.CLASS, 10, 10);
        stack.push(Commands.addNode(diagram, node));
        stack.push(Commands.property(diagram, node, "name", "Paid"));
        check(diagram.nodes().size() == 1 && node.get("name").equals("Paid"), "edited");
        stack.undo();
        check("Class".equals(node.get("name")), "undo property");
        stack.undo();
        check(diagram.nodes().isEmpty(), "undo to the initial diagram");
        stack.redo();
        check(diagram.nodes().size() == 1, "redo node");
        check(!stack.isDirty() == false, "dirty after edit");
        stack.markSaved();
        check(!stack.isDirty(), "clean after save");
    }

    private static void geometry() {
        Diagram diagram = sampleClass();
        Layout before = Layout.of(diagram, new Measure(new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
        Edge edge = diagram.edges().get(0);
        int x = edge.end().x();
        edge.end().moveTo(x + 80, edge.end().y() + 40);
        Layout after = Layout.of(diagram, new Measure(new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
        check(!before.path(edge).equals(after.path(edge)), "edge follows the node");
        check(before.bounds(diagram.nodes().get(0)).width > 40, "node has a size");
    }

    private static void renderAll() throws Exception {
        Path directory = Path.of("out", "preview");
        Files.createDirectories(directory);
        save(sampleClass(), directory.resolve("class.png"));
        save(sampleSequence(), directory.resolve("sequence.png"));
        save(sampleState(), directory.resolve("state.png"));
        save(sampleObject(), directory.resolve("object.png"));
        save(sampleUseCase(), directory.resolve("usecase.png"));
        String svg = Exporter.svg(sampleClass(), new Font(Font.SANS_SERIF, Font.PLAIN, 14), false);
        check(svg.contains("<svg") && svg.contains("</svg>") && !svg.contains("grid"), "svg export");
        Files.writeString(directory.resolve("class.svg"), svg);
    }

    private static void save(Diagram diagram, Path path) throws Exception {
        BufferedImage image = Exporter.image(diagram, new Font(Font.SANS_SERIF, Font.PLAIN, 14), false);
        check(image.getWidth() > 20 && image.getHeight() > 20, path.getFileName() + " rendered");
        ImageIO.write(image, "png", path.toFile());
    }

    private static Diagram sampleClass() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        Node pkg = node(diagram, NodeType.PACKAGE, 30, 30);
        pkg.set("name", "billing");
        Node customer = node(diagram, NodeType.CLASS, 50, 80);
        customer.setParent(pkg);
        customer.set("name", "/Customer/");
        customer.set("attributes", "name: String\n_id_: int");
        customer.set("methods", "/total/(): double");
        Node order = node(diagram, NodeType.CLASS, 280, 80);
        order.set("name", "Order");
        order.set("attributes", "items: List");
        order.set("methods", "add()");
        Node face = node(diagram, NodeType.INTERFACE, 280, 250);
        face.set("name", "Payable");
        face.set("methods", "pay()");
        Edge association = edge(diagram, EdgeKind.ASSOCIATION, customer, order);
        association.set("startLabel", "1");
        association.set("endLabel", "*");
        association.set("middleLabel", "places");
        association.set("directionality", "Unidirectional");
        edge(diagram, EdgeKind.REALIZATION, order, face);
        Node note = node(diagram, NodeType.NOTE, 40, 260);
        note.set("text", "Aggregate root");
        edge(diagram, EdgeKind.NOTE, customer, note);
        edge(diagram, EdgeKind.ASSOCIATION, customer, customer).set("middleLabel", "parent");
        return diagram;
    }

    private static Diagram sampleSequence() {
        Diagram diagram = new Diagram(DiagramType.SEQUENCE);
        Node ui = node(diagram, NodeType.IMPLICIT_PARAMETER, 60, 30);
        ui.set("name", "ui:View");
        Node service = node(diagram, NodeType.IMPLICIT_PARAMETER, 260, 30);
        service.set("name", "svc:Service");
        Node repo = node(diagram, NodeType.IMPLICIT_PARAMETER, 460, 30);
        repo.set("name", "db:Repo");
        SequenceGestures.Result found = SequenceGestures.create(diagram, ui, false, service, false, 130);
        found.command().execute();
        Node caller = diagram.callerOf(found.focus());
        SequenceGestures.Result next = SequenceGestures.create(diagram, found.focus(), false, repo, false, 150);
        if (next.command() != null) {
            next.command().execute();
            next.focus().set("openBottom", "true");
        }
        int selfY = caller.y() + SequenceGestures.height(diagram, caller) - 8;
        SequenceGestures.Result self = SequenceGestures.create(diagram, caller, false, caller, false, selfY);
        if (self.command() != null) {
            self.command().execute();
        }
        SequenceGestures.Result back = SequenceGestures.createReturn(diagram, found.focus(), caller);
        if (back.command() != null) {
            back.command().execute();
        }
        found.focus();
        for (var edge : diagram.edges()) {
            if (edge.kind() == EdgeKind.CALL && edge.end() == found.focus()) {
                edge.set("middleLabel", "checkout()");
            }
        }
        return diagram;
    }

    private static Diagram sampleState() {
        Diagram diagram = new Diagram(DiagramType.STATE);
        Node initial = node(diagram, NodeType.INITIAL_STATE, 40, 80);
        Node open = node(diagram, NodeType.STATE, 140, 60);
        open.set("name", "Open");
        Node done = node(diagram, NodeType.FINAL_STATE, 340, 70);
        edge(diagram, EdgeKind.TRANSITION, initial, open);
        Edge go = edge(diagram, EdgeKind.TRANSITION, open, done);
        go.set("middleLabel", "close");
        edge(diagram, EdgeKind.TRANSITION, open, open).set("middleLabel", "edit");
        return diagram;
    }

    private static Diagram sampleObject() {
        Diagram diagram = new Diagram(DiagramType.OBJECT);
        Node cart = node(diagram, NodeType.OBJECT, 40, 40);
        cart.set("name", "cart:Cart");
        Node item = node(diagram, NodeType.FIELD, 40, 80);
        item.setParent(cart);
        item.set("name", "total");
        item.set("value", "12.00");
        Node product = node(diagram, NodeType.OBJECT, 260, 70);
        product.set("name", "book:Product");
        edge(diagram, EdgeKind.REFERENCE, item, product);
        Node other = node(diagram, NodeType.OBJECT, 260, 200);
        other.set("name", "user:User");
        edge(diagram, EdgeKind.COLLABORATION, cart, other).set("middleLabel", "owner");
        return diagram;
    }

    private static Diagram sampleUseCase() {
        Diagram diagram = new Diagram(DiagramType.USE_CASE);
        Node guest = node(diagram, NodeType.ACTOR, 30, 80);
        guest.set("name", "Guest");
        Node member = node(diagram, NodeType.ACTOR, 30, 230);
        member.set("name", "Member");
        Node login = node(diagram, NodeType.USE_CASE, 220, 40);
        login.set("name", "Log in");
        Node buy = node(diagram, NodeType.USE_CASE, 220, 160);
        buy.set("name", "Buy book");
        edge(diagram, EdgeKind.GENERALIZATION, member, guest);
        edge(diagram, EdgeKind.USE_ASSOCIATION, guest, login);
        edge(diagram, EdgeKind.INCLUDE, buy, login);
        edge(diagram, EdgeKind.EXTEND, login, buy);
        return diagram;
    }

    private static Node node(Diagram diagram, NodeType type, int x, int y) {
        Node node = Node.create(diagram.nextNodeId(), type, x, y);
        diagram.addNode(node);
        return node;
    }

    private static Edge edge(Diagram diagram, EdgeKind kind, Node start, Node end) {
        Edge edge = new Edge(diagram.nextEdgeId(), kind, start, end);
        diagram.addEdge(edge);
        return edge;
    }

    private static void add(Diagram diagram, EdgeKind kind, Node start, Node end) {
        diagram.addEdge(new Edge(diagram.nextEdgeId(), kind, start, end));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            failures++;
            System.out.println("FAIL " + message);
        }
    }
}
