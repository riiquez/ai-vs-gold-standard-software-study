package jetuml.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jetuml.app.Tool;
import jetuml.cmd.UndoStack;
import jetuml.controller.CanvasController;
import jetuml.controller.CanvasHost;
import jetuml.model.Diagram;
import jetuml.model.Element;
import jetuml.view.DiagramPainter;
import jetuml.view.Layout;
import jetuml.view.Measure;
import jetuml.view.SwingDraw;
import jetuml.view.Theme;

/** Visão do diagrama. Traduz mouse e teclado e deixa o controlador decidir a ação. */
public final class DiagramCanvas extends JPanel implements CanvasHost {
    private static final double[] ZOOMS = {0.5, 0.75, 1.0, 1.25, 1.5};

    private final Diagram diagram;
    private final EditorFrame.Session session;
    private final DiagramPainter painter = new DiagramPainter();
    private final CanvasController input;
    private final Timer createTimer;
    private int zoomIndex = 2;
    private Layout layout;
    private int layoutRevision = -1;
    private Font layoutFont;

    public DiagramCanvas(Diagram diagram, EditorFrame.Session session) {
        this.diagram = diagram;
        this.session = session;
        this.input = new CanvasController(this);
        setBackground(Color.WHITE);
        setFocusable(true);
        createTimer = new Timer(260, event -> input.createPendingNode());
        createTimer.setRepeats(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (event.isPopupTrigger()) {
                    showTools(event);
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                if (event.getClickCount() >= 2) {
                    createTimer.stop();
                    input.cancelPendingCreate();
                }
                input.press(modelX(event), modelY(event), event.getX(), event.getY(), event.isControlDown(), event.getClickCount());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showTools(event);
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(event)) {
                    input.release(modelX(event), modelY(event), event.getX(), event.getY(),
                            event.isControlDown(), event.getClickCount());
                    if (input.hasPendingCreate()) {
                        createTimer.restart();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    createTimer.stop();
                    input.drag(modelX(event), modelY(event), event.getX(), event.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2 && SwingUtilities.isLeftMouseButton(event)) {
                    createTimer.stop();
                    input.openAt(modelX(event), modelY(event));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(event -> {
            if (event.isControlDown()) {
                if (event.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        });
        diagram.addListener(changed -> refresh());
        syncSize();
    }

    public Diagram diagram() {
        return diagram;
    }

    public Set<Element> selection() {
        return input.selection();
    }

    public int zoomPercent() {
        return (int) Math.round(ZOOMS[zoomIndex] * 100);
    }

    public void zoomIn() {
        if (zoomIndex < ZOOMS.length - 1) {
            zoomIndex++;
            syncSize();
        }
    }

    public void zoomOut() {
        if (zoomIndex > 0) {
            zoomIndex--;
            syncSize();
        }
    }

    public void resetZoom() {
        zoomIndex = 2;
        syncSize();
    }

    public boolean canZoomIn() {
        return zoomIndex < ZOOMS.length - 1;
    }

    public boolean canZoomOut() {
        return zoomIndex > 0;
    }

    public void syncSize() {
        double zoom = ZOOMS[zoomIndex];
        setPreferredSize(new Dimension(
                (int) Math.ceil(session.modelWidth() * zoom),
                (int) Math.ceil(session.modelHeight() * zoom)));
        revalidate();
        repaint();
    }

    public void refresh() {
        input.refreshSelection();
        layout = null;
        repaint();
    }

    public void selectAll() {
        input.selectAll();
    }

    public void clearSelection() {
        input.clearSelection();
    }

    public void shiftReparent() {
        input.shiftReparent();
    }

    @Override
    public Tool tool() {
        return session.tool();
    }

    @Override
    public UndoStack stack() {
        return session.stack();
    }

    @Override
    public void changed() {
        session.changed();
    }

    @Override
    public void notifyUser(String message, boolean error) {
        session.frame().notify(message, error);
    }

    @Override
    public void openProperties(Element element) {
        session.openProperties(element);
    }

    @Override
    public boolean autoEdit() {
        return session.frame().preferences().autoEditNode();
    }

    @Override
    public int modelWidth() {
        return session.modelWidth();
    }

    @Override
    public int modelHeight() {
        return session.modelHeight();
    }

    @Override
    public Layout geometry() {
        return view();
    }

    @Override
    public void repaintCanvas() {
        repaint();
    }

    private void showTools(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        Tool[] tools = Tool.forDiagram(diagram.type());
        for (int i = 0; i < tools.length; i++) {
            Tool tool = tools[i];
            JMenuItem item = new JMenuItem(tool.label() + "    " + Tool.shortcut(i));
            if (tool == session.tool()) {
                item.setFont(item.getFont().deriveFont(Font.BOLD));
            }
            item.addActionListener(action -> session.frame().selectTool(tool));
            menu.add(item);
        }
        menu.show(this, event.getX(), event.getY());
    }

    private int modelX(MouseEvent event) {
        return (int) Math.round(event.getX() / ZOOMS[zoomIndex]);
    }

    private int modelY(MouseEvent event) {
        return (int) Math.round(event.getY() / ZOOMS[zoomIndex]);
    }

    private Layout view() {
        Font font = session.frame().preferences().diagramFont();
        if (layout == null || layoutRevision != diagram.revision() || !font.equals(layoutFont)) {
            layout = Layout.of(diagram, new Measure(font));
            layoutRevision = diagram.revision();
            layoutFont = font;
        }
        return layout;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Theme theme = new Theme(session.frame().preferences().darkMode());
        g.setColor(theme.canvas());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.scale(ZOOMS[zoomIndex], ZOOMS[zoomIndex]);
        painter.paint(new SwingDraw(g), view(), theme,
                session.frame().preferences().showGrid(),
                session.modelWidth(), session.modelHeight(), input.selection(), input.rubber(), input.band());
        g.dispose();
    }
}
