package jetuml.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import jetuml.app.ElementClipboard;
import jetuml.app.Preferences;
import jetuml.app.Tool;
import jetuml.cmd.UndoStack;
import jetuml.controller.EditingSession;
import jetuml.controller.EditorController;
import jetuml.model.Diagram;
import jetuml.model.DiagramType;
import jetuml.model.Element;
import jetuml.model.Geom;
import jetuml.persist.DiagramCodec;
import jetuml.view.Exporter;
import jetuml.view.Layout;
import jetuml.view.Measure;
import jetuml.view.Theme;

public final class EditorFrame extends JFrame {
    private final Preferences preferences = new Preferences();
    private final EditorController editing = new EditorController();
    private final List<Session> sessions = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel cards = new JPanel(new CardLayout());
    private final JPanel welcome = new JPanel(new GridBagLayout());
    private final JPanel toolBar = new JPanel();
    private final JLabel status = new JLabel("JetUML");
    private final NoticeGlass notice = new NoticeGlass();
    private final Map<Tool, AbstractButton> toolButtons = new EnumMap<>(Tool.class);
    private ButtonGroup toolGroup = new ButtonGroup();
    private Tool currentTool = Tool.SELECT;
    private PropertyDialog properties;

    private JMenuItem undoItem;
    private JMenuItem redoItem;
    private JMenuItem cutItem;
    private JMenuItem copyItem;
    private JMenuItem pasteItem;
    private JMenuItem deleteItem;
    private JMenuItem propertiesItem;
    private JMenuItem closeItem;
    private JMenuItem saveItem;
    private JMenuItem saveAsItem;
    private JMenuItem duplicateItem;
    private JMenuItem exportItem;
    private JMenuItem copyImageItem;
    private JMenuItem zoomInItem;
    private JMenuItem zoomOutItem;
    private JMenu recentMenu;
    private JCheckBoxMenuItem gridItem;
    private JCheckBoxMenuItem darkItem;
    private JCheckBoxMenuItem hintItem;
    private JCheckBoxMenuItem verboseItem;
    private JCheckBoxMenuItem autoEditItem;

    public EditorFrame() {
        super("JetUML");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        buildWelcome();
        cards.add(welcome, "welcome");
        cards.add(tabs, "tabs");
        JScrollPane tools = new JScrollPane(toolBar);
        tools.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tools.setBorder(BorderFactory.createEmptyBorder());
        add(tools, BorderLayout.WEST);
        add(cards, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        add(status, BorderLayout.SOUTH);
        setJMenuBar(buildMenu());
        setGlassPane(notice);
        installKeys();
        tabs.addChangeListener(event -> {
            adaptToolToDiagram();
            rebuildTools();
            refreshChrome();
            Session session = active();
            if (session != null) {
                session.canvas.requestFocusInWindow();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                exit();
            }
        });
        applyTheme();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1280, screen.width - 80), Math.min(820, screen.height - 80));
        setLocationRelativeTo(null);
        refreshChrome();
    }

    public Preferences preferences() {
        return preferences;
    }

    public Tool tool() {
        return currentTool;
    }

    public void notify(String message, boolean error) {
        notice.showNotice(message, error, preferences.notificationSeconds());
    }

    public void selectTool(Tool tool) {
        if (tool == null) {
            return;
        }
        if (tool.isAction()) {
            copyImage();
            return;
        }
        currentTool = tool;
        AbstractButton button = toolButtons.get(tool);
        if (button != null && !button.isSelected()) {
            button.setSelected(true);
        }
        Session session = active();
        if (session != null) {
            session.canvas.requestFocusInWindow();
        }
    }

    private void installKeys() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            int modifiers = event.getModifiersEx();
            boolean ctrl = (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0;
            boolean alt = (modifiers & KeyEvent.ALT_DOWN_MASK) != 0;
            boolean meta = (modifiers & KeyEvent.META_DOWN_MASK) != 0;
            boolean shift = (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0;
            Window focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            boolean ownedDialog = focused instanceof JDialog dialog && dialog.getOwner() == this;
            if (ctrl && !alt && !meta && !shift && event.getKeyCode() == KeyEvent.VK_Q) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof JTextComponent text && text.isEditable()) {
                    PropertyDialog.insertGuillemets(text);
                    return true;
                }
                if (focused == this) {
                    exit();
                    return true;
                }
                return false;
            }
            if (ownedDialog && ctrl && !alt && !meta && !shift) {
                Session session = active();
                if (session == null) {
                    return false;
                }
                return switch (event.getKeyCode()) {
                    case KeyEvent.VK_S -> {
                        save(session, false);
                        yield true;
                    }
                    case KeyEvent.VK_Z -> {
                        undo();
                        yield true;
                    }
                    case KeyEvent.VK_Y -> {
                        redo();
                        yield true;
                    }
                    case KeyEvent.VK_B -> {
                        copyImage();
                        yield true;
                    }
                    default -> false;
                };
            }
            if (focused != this || ctrl || alt || meta) {
                return false;
            }
            Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focus instanceof JTextComponent || SwingUtilities.getAncestorOfClass(JSpinner.class, focus) != null) {
                return false;
            }
            Session session = active();
            if (session == null) {
                return false;
            }
            if (event.getKeyCode() == KeyEvent.VK_SHIFT && (modifiers & KeyEvent.BUTTON1_DOWN_MASK) == 0) {
                session.canvas.shiftReparent();
                return false;
            }
            if (shift) {
                return false;
            }
            String shortcut = shortcut(event.getKeyCode());
            if (shortcut == null) {
                return false;
            }
            Tool[] tools = Tool.forDiagram(session.diagram.type());
            for (int i = 0; i < tools.length; i++) {
                if (Tool.shortcut(i).equals(shortcut)) {
                    selectTool(tools[i]);
                    return true;
                }
            }
            return false;
        });
    }

    private static String shortcut(int keyCode) {
        if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
            return Integer.toString(keyCode - KeyEvent.VK_1 + 1);
        }
        if (keyCode == KeyEvent.VK_0) {
            return "0";
        }
        if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) keyCode);
        }
        return null;
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenu newer = new JMenu("New");
        for (DiagramType type : DiagramType.values()) {
            JMenuItem item = new JMenuItem(type.title());
            item.addActionListener(event -> newDiagram(type));
            newer.add(item);
        }
        file.add(newer);
        JMenuItem open = item("Open...", KeyEvent.VK_O, this::open);
        file.add(open);
        recentMenu = new JMenu("Recent Files");
        recentMenu.addMenuListener(new javax.swing.event.MenuListener() {
            @Override
            public void menuSelected(javax.swing.event.MenuEvent event) {
                rebuildRecent();
            }

            @Override
            public void menuDeselected(javax.swing.event.MenuEvent event) {
            }

            @Override
            public void menuCanceled(javax.swing.event.MenuEvent event) {
            }
        });
        file.add(recentMenu);
        closeItem = item("Close", KeyEvent.VK_W, () -> {
            Session session = active();
            if (session != null) {
                close(session);
            }
        });
        file.add(closeItem);
        file.addSeparator();
        saveItem = item("Save", KeyEvent.VK_S, () -> {
            Session session = active();
            if (session != null) {
                save(session, false);
            }
        });
        saveAsItem = item("Save As...", 0, () -> {
            Session session = active();
            if (session != null) {
                save(session, true);
            }
        });
        file.add(saveItem);
        file.add(saveAsItem);
        duplicateItem = item("Duplicate", KeyEvent.VK_D, this::duplicate);
        file.add(duplicateItem);
        file.addSeparator();
        exportItem = item("Export Image...", 0, this::exportImage);
        copyImageItem = item("Copy to Clipboard", KeyEvent.VK_B, this::copyImage);
        file.add(exportItem);
        file.add(copyImageItem);
        file.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exit.addActionListener(event -> exit());
        file.add(exit);

        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        undoItem = item("Undo", KeyEvent.VK_Z, this::undo);
        redoItem = item("Redo", KeyEvent.VK_Y, this::redo);
        edit.add(undoItem);
        edit.add(redoItem);
        edit.addSeparator();
        cutItem = item("Cut", KeyEvent.VK_X, this::cut);
        copyItem = item("Copy", KeyEvent.VK_C, this::copyElements);
        pasteItem = item("Paste", KeyEvent.VK_V, this::paste);
        edit.add(cutItem);
        edit.add(copyItem);
        edit.add(pasteItem);
        edit.addSeparator();
        JMenuItem selectAll = item("Select All", KeyEvent.VK_A, () -> {
            Session session = active();
            if (session != null) {
                session.canvas.selectAll();
                refreshChrome();
            }
        });
        deleteItem = new JMenuItem("Delete");
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteItem.addActionListener(event -> deleteSelection());
        propertiesItem = new JMenuItem("Properties");
        propertiesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK));
        propertiesItem.addActionListener(event -> editProperties());
        edit.add(selectAll);
        edit.add(deleteItem);
        edit.add(propertiesItem);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        gridItem = check("Show Grid", () -> {
            preferences.setShowGrid(gridItem.isSelected());
            repaintCanvases();
        });
        zoomInItem = item("Zoom In", KeyEvent.VK_EQUALS, () -> {
            Session session = active();
            if (session != null) {
                session.canvas.zoomIn();
                refreshChrome();
            }
        });
        zoomOutItem = item("Zoom Out", KeyEvent.VK_MINUS, () -> {
            Session session = active();
            if (session != null) {
                session.canvas.zoomOut();
                refreshChrome();
            }
        });
        JMenuItem resetZoom = item("Reset Zoom", KeyEvent.VK_0, () -> {
            Session session = active();
            if (session != null) {
                session.canvas.resetZoom();
                refreshChrome();
            }
        });
        registerZoomKeys();
        view.add(gridItem);
        view.addSeparator();
        view.add(zoomInItem);
        view.add(zoomOutItem);
        view.add(resetZoom);
        view.addSeparator();
        view.add(item("Set Diagram Size...", 0, this::chooseSize));
        view.add(item("Change Font...", 0, this::chooseFont));
        view.addSeparator();
        darkItem = check("Dark Mode", () -> {
            preferences.setDarkMode(darkItem.isSelected());
            applyTheme();
        });
        hintItem = check("Show Tool Hint", () -> {
            preferences.setShowToolHint(hintItem.isSelected());
            rebuildTools();
        });
        verboseItem = check("Verbose Tooltips", () -> {
            preferences.setVerboseTooltips(verboseItem.isSelected());
            rebuildTools();
        });
        autoEditItem = check("Auto Edit Node", () -> preferences.setAutoEditNode(autoEditItem.isSelected()));
        view.add(darkItem);
        view.add(hintItem);
        view.add(verboseItem);
        view.add(autoEditItem);
        view.addSeparator();
        view.add(item("Notification Duration...", 0, this::chooseDuration));
        view.addMenuListener(new javax.swing.event.MenuListener() {
            @Override
            public void menuSelected(javax.swing.event.MenuEvent event) {
                gridItem.setSelected(preferences.showGrid());
                darkItem.setSelected(preferences.darkMode());
                hintItem.setSelected(preferences.showToolHint());
                verboseItem.setSelected(preferences.verboseTooltips());
                autoEditItem.setSelected(preferences.autoEditNode());
            }

            @Override
            public void menuDeselected(javax.swing.event.MenuEvent event) {
            }

            @Override
            public void menuCanceled(javax.swing.event.MenuEvent event) {
            }
        });
        bar.add(file);
        bar.add(edit);
        bar.add(view);
        return bar;
    }

    private void registerZoomKeys() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK), "zoom-in");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK), "zoom-in");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK), "zoom-out");
        root.getActionMap().put("zoom-in", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                Session session = active();
                if (session != null) {
                    session.canvas.zoomIn();
                    refreshChrome();
                }
            }
        });
        root.getActionMap().put("zoom-out", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                Session session = active();
                if (session != null) {
                    session.canvas.zoomOut();
                    refreshChrome();
                }
            }
        });
    }

    private void buildWelcome() {
        welcome.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.insets = new Insets(6, 8, 6, 8);
        JLabel title = new JLabel("JetUML");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        welcome.add(title, constraints);
        JLabel subtitle = new JLabel("Choose a diagram to start");
        welcome.add(subtitle, constraints);
        welcome.add(Box.createVerticalStrut(8), constraints);
        for (DiagramType type : DiagramType.values()) {
            JButton button = new JButton(type.title());
            button.setFont(button.getFont().deriveFont(16f));
            button.addActionListener(event -> newDiagram(type));
            welcome.add(button, constraints);
        }
    }

    private void newDiagram(DiagramType type) {
        addSession(new Diagram(type), null, uniqueTitle(type.title()), preferences.diagramWidth(), preferences.diagramHeight());
    }

    private String uniqueTitle(String base) {
        String name = base;
        int number = 2;
        while (titleTaken(name)) {
            name = base + " " + number++;
        }
        return name;
    }

    private boolean titleTaken(String name) {
        for (Session session : sessions) {
            if (session.file == null && session.title.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Session addSession(Diagram diagram, File file, String title, int width, int height) {
        Session session = new Session(this, diagram, file, title, width, height);
        sessions.add(session);
        JScrollPane scroll = new JScrollPane(session.canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        tabs.addTab(session.tabTitle(), scroll);
        tabs.setTabComponentAt(sessions.size() - 1, tabHeader(session));
        tabs.setSelectedIndex(sessions.size() - 1);
        showCards();
        adaptToolToDiagram();
        rebuildTools();
        refreshChrome();
        session.canvas.requestFocusInWindow();
        return session;
    }

    private JComponent tabHeader(Session session) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel label = new JLabel(session.tabTitle());
        session.tabLabel = label;
        JButton close = new JButton("×");
        close.setMargin(new Insets(0, 2, 0, 2));
        close.setFocusable(false);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.addActionListener(event -> close(session));
        header.add(label);
        header.add(close);
        return header;
    }

    private void showCards() {
        ((CardLayout) cards.getLayout()).show(cards, sessions.isEmpty() ? "welcome" : "tabs");
    }

    private Session active() {
        int index = tabs.getSelectedIndex();
        if (index < 0 || index >= sessions.size()) {
            return null;
        }
        return sessions.get(index);
    }

    private void open() {
        JFileChooser chooser = chooser("Open");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON diagram (*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            remember(chooser);
            openFile(chooser.getSelectedFile());
        }
    }

    private void openFile(File file) {
        if (file == null || !file.isFile()) {
            notify("File not found.", true);
            return;
        }
        try {
            Diagram diagram = DiagramCodec.decode(Files.readString(file.toPath()));
            java.awt.Rectangle content = Layout.of(diagram, new Measure(preferences.diagramFont())).contentBounds();
            int width = Geom.clampDiagram(Math.max(preferences.diagramWidth(), (int) Math.ceil(content.getMaxX()) + 48));
            int height = Geom.clampDiagram(Math.max(preferences.diagramHeight(), (int) Math.ceil(content.getMaxY()) + 48));
            addSession(diagram, file, file.getName(), width, height).stack.markSaved();
            preferences.rememberFile(file.getAbsolutePath());
            refreshChrome();
        } catch (Exception exception) {
            notify("Could not open the file. " + exception.getMessage(), true);
        }
    }

    private void rebuildRecent() {
        recentMenu.removeAll();
        List<String> files = preferences.recentFiles();
        if (files.isEmpty()) {
            JMenuItem empty = new JMenuItem("(empty)");
            empty.setEnabled(false);
            recentMenu.add(empty);
            return;
        }
        for (String path : files) {
            JMenuItem item = new JMenuItem(path);
            item.addActionListener(event -> openFile(new File(path)));
            recentMenu.add(item);
        }
    }

    private boolean save(Session session, boolean forceDialog) {
        File target = session.file;
        if (target == null || forceDialog) {
            JFileChooser chooser = chooser("Save");
            chooser.setSelectedFile(new File(strip(session.displayName()) + ".json"));
            chooser.setFileFilter(new FileNameExtensionFilter("JSON diagram (*.json)", "json"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            remember(chooser);
            target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".json")) {
                target = new File(target.getParentFile(), target.getName() + ".json");
            }
        }
        try {
            Files.writeString(target.toPath(), DiagramCodec.encode(session.diagram));
            session.file = target;
            session.stack.markSaved();
            preferences.rememberFile(target.getAbsolutePath());
            refreshChrome();
            return true;
        } catch (IOException exception) {
            notify("Could not save the file.", true);
            return false;
        }
    }

    private boolean close(Session session) {
        if (session.stack.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this, "Save changes to " + session.displayName() + "?",
                    "JetUML", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
            if (choice == JOptionPane.YES_OPTION && !save(session, false)) {
                return false;
            }
        }
        int index = sessions.indexOf(session);
        if (index >= 0) {
            sessions.remove(index);
            tabs.remove(index);
        }
        if (properties != null) {
            properties.dispose();
            properties = null;
        }
        showCards();
        rebuildTools();
        refreshChrome();
        return true;
    }

    private void duplicate() {
        Session session = active();
        if (session == null) {
            return;
        }
        try {
            Diagram copy = DiagramCodec.decode(DiagramCodec.encode(session.diagram));
            Session created = addSession(copy, null, uniqueTitle(session.title + " copy"), session.width, session.height);
            created.stack.markUnsaved();
            refreshChrome();
        } catch (RuntimeException exception) {
            notify("Could not duplicate the diagram.", true);
        }
    }

    private void exportImage() {
        Session session = active();
        if (session == null) {
            return;
        }
        JFileChooser chooser = chooser("Export Image");
        List<String> suffixes = new ArrayList<>();
        suffixes.add("svg");
        for (String suffix : ImageIO.getWriterFileSuffixes()) {
            String lower = suffix.toLowerCase();
            if (!suffixes.contains(lower)) {
                suffixes.add(lower);
            }
        }
        chooser.setAcceptAllFileFilterUsed(false);
        for (String suffix : suffixes) {
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(suffix.toUpperCase() + " (*." + suffix + ")", suffix));
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        remember(chooser);
        File file = chooser.getSelectedFile();
        String extension = extension(file.getName());
        if (extension.isEmpty() && chooser.getFileFilter() instanceof FileNameExtensionFilter filter && filter.getExtensions().length > 0) {
            extension = filter.getExtensions()[0].toLowerCase();
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        try {
            if ("svg".equals(extension)) {
                Files.writeString(file.toPath(), Exporter.svg(session.diagram, preferences.diagramFont(), preferences.darkMode()));
            } else {
                String format = "jpg".equals(extension) ? "jpeg" : extension;
                boolean written = ImageIO.write(Exporter.image(session.diagram, preferences.diagramFont(), preferences.darkMode()), format, file);
                if (!written) {
                    notify("This image format is not available.", true);
                    return;
                }
            }
            notify("Image exported.", false);
        } catch (IOException exception) {
            notify("Could not export the image.", true);
        }
    }

    private void copyImage() {
        Session session = active();
        if (session == null) {
            return;
        }
        Image image = Exporter.image(session.diagram, preferences.diagramFont(), preferences.darkMode());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
        notify("Diagram copied to clipboard.", false);
    }

    private void undo() {
        Session session = active();
        if (session != null) {
            editing.undo(session);
        }
    }

    private void redo() {
        Session session = active();
        if (session != null) {
            editing.redo(session);
        }
    }

    private void cut() {
        Session session = active();
        if (session != null) {
            editing.cut(session);
        }
    }

    private void copyElements() {
        Session session = active();
        if (session != null) {
            editing.copy(session);
        }
    }

    private void paste() {
        Session session = active();
        if (session != null) {
            editing.paste(session);
        }
    }

    private void deleteSelection() {
        Session session = active();
        if (session != null) {
            editing.delete(session);
        }
    }

    private void editProperties() {
        Session session = active();
        if (session != null) {
            editing.editProperties(session);
        }
    }

    void openProperties(Session session, Element element) {
        if (properties != null) {
            properties.dispose();
        }
        properties = new PropertyDialog(this, session, element);
        properties.setVisible(true);
    }

    private void exit() {
        for (int i = sessions.size() - 1; i >= 0; i--) {
            if (!close(sessions.get(i))) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }

    private void chooseSize() {
        int width = preferences.diagramWidth();
        int height = preferences.diagramHeight();
        JSpinner widthSpinner = spinner(width, Geom.MIN_DIAGRAM, Geom.MAX_DIAGRAM);
        JSpinner heightSpinner = spinner(height, Geom.MIN_DIAGRAM, Geom.MAX_DIAGRAM);
        JPanel panel = form(widthSpinner, heightSpinner, "Width", "Height");
        String[] options = {"OK", "Restore Defaults", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, panel, "Set Diagram Size",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 1) {
            preferences.restoreDiagramSize();
            applySize(preferences.diagramWidth(), preferences.diagramHeight());
        } else if (choice == 0) {
            preferences.setDiagramSize(((Number) widthSpinner.getValue()).intValue(), ((Number) heightSpinner.getValue()).intValue());
            applySize(preferences.diagramWidth(), preferences.diagramHeight());
        }
    }

    private void applySize(int width, int height) {
        for (Session session : sessions) {
            java.awt.Rectangle content = Layout.of(session.diagram, new Measure(preferences.diagramFont())).contentBounds();
            session.width = Geom.clampDiagram(Math.max(width, (int) Math.ceil(content.getMaxX()) + 48));
            session.height = Geom.clampDiagram(Math.max(height, (int) Math.ceil(content.getMaxY()) + 48));
            session.canvas.syncSize();
        }
        refreshChrome();
    }

    private void chooseFont() {
        Font current = preferences.diagramFont();
        JComboBox<String> families = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        families.setSelectedItem(current.getFamily());
        JSpinner size = spinner(current.getSize(), 8, 72);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Family"), constraints);
        constraints.gridx = 1;
        panel.add(families, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(new JLabel("Size"), constraints);
        constraints.gridx = 1;
        panel.add(size, constraints);
        String[] options = {"OK", "Restore Default", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, panel, "Change Font",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 1) {
            preferences.restoreFont();
        } else if (choice == 0) {
            preferences.setFont(String.valueOf(families.getSelectedItem()), ((Number) size.getValue()).intValue());
        } else {
            return;
        }
        repaintCanvases();
    }

    private void chooseDuration() {
        JSpinner seconds = spinner(preferences.notificationSeconds(), 0, 10);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        panel.add(new JLabel("Seconds (0 disables notifications)"), constraints);
        panel.add(seconds, constraints);
        String[] options = {"OK", "Restore Default", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, panel, "Notification Duration",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 1) {
            preferences.restoreNotificationSeconds();
        } else if (choice == 0) {
            preferences.setNotificationSeconds(((Number) seconds.getValue()).intValue());
        }
        if (preferences.notificationSeconds() == 0) {
            notice.hideNotice();
        }
    }

    private void adaptToolToDiagram() {
        Session session = active();
        if (session == null) {
            currentTool = Tool.SELECT;
            return;
        }
        for (Tool tool : Tool.forDiagram(session.diagram.type())) {
            if (tool == currentTool) {
                return;
            }
        }
        currentTool = Tool.SELECT;
    }

    private void rebuildTools() {
        toolBar.removeAll();
        toolButtons.clear();
        toolGroup = new ButtonGroup();
        Session session = active();
        if (session == null) {
            toolBar.revalidate();
            toolBar.repaint();
            return;
        }
        Color ink = preferences.darkMode() ? new Color(0xF2F2F2) : new Color(0x202124);
        for (Tool tool : Tool.forDiagram(session.diagram.type())) {
            AbstractButton button = tool.isAction()
                    ? new JButton(preferences.showToolHint() ? tool.label() : null, new ToolIcon(tool, ink))
                    : new JToggleButton(preferences.showToolHint() ? tool.label() : null, new ToolIcon(tool, ink));
            button.setToolTipText(preferences.verboseTooltips() ? tool.verbose() : tool.label());
            button.setFocusable(false);
            button.setHorizontalAlignment(SwingUtilities.LEFT);
            if (button instanceof JToggleButton toggle) {
                toolGroup.add(toggle);
                if (tool == currentTool) {
                    toggle.setSelected(true);
                }
                toggle.addActionListener(event -> {
                    if (toggle.isSelected()) {
                        selectTool(tool);
                    }
                });
            } else {
                button.addActionListener(event -> selectTool(tool));
            }
            toolButtons.put(tool, button);
            toolBar.add(button);
            toolBar.add(Box.createVerticalStrut(2));
        }
        toolBar.revalidate();
        toolBar.repaint();
    }

    void refreshChrome() {
        Session session = active();
        for (Session each : sessions) {
            if (each.tabLabel != null) {
                each.tabLabel.setText(each.tabTitle());
            }
        }
        setTitle(session == null ? "JetUML" : "JetUML — " + session.tabTitle());
        boolean open = session != null;
        closeItem.setEnabled(open);
        saveItem.setEnabled(open);
        saveAsItem.setEnabled(open);
        duplicateItem.setEnabled(open);
        exportItem.setEnabled(open);
        copyImageItem.setEnabled(open);
        undoItem.setEnabled(open && session.stack.canUndo());
        redoItem.setEnabled(open && session.stack.canRedo());
        boolean selection = open && !session.canvas.selection().isEmpty();
        cutItem.setEnabled(selection);
        copyItem.setEnabled(selection);
        deleteItem.setEnabled(selection);
        propertiesItem.setEnabled(open && session.canvas.selection().size() == 1);
        pasteItem.setEnabled(open && ElementClipboard.hasContent());
        zoomInItem.setEnabled(open && session.canvas.canZoomIn());
        zoomOutItem.setEnabled(open && session.canvas.canZoomOut());
        if (properties != null && properties.isVisible() && open) {
            Element element = properties.element();
            boolean gone = element instanceof jetuml.model.Node node
                    ? !session.diagram.nodes().contains(node)
                    : element instanceof jetuml.model.Edge edge && !session.diagram.edges().contains(edge);
            if (gone) {
                properties.dispose();
            }
        }
        if (open) {
            status.setText(session.diagram.type().title() + "    " + session.canvas.zoomPercent()
                    + "%    " + session.width + " × " + session.height);
            Theme theme = new Theme(preferences.darkMode());
            session.canvas.setBackground(theme.canvas());
        } else {
            status.setText("JetUML");
        }
        showCards();
    }

    private void repaintCanvases() {
        for (Session session : sessions) {
            session.canvas.syncSize();
        }
        refreshChrome();
    }

    private void applyTheme() {
        boolean dark = preferences.darkMode();
        Color background = dark ? new Color(0x2B2D31) : new Color(0xF4F5F7);
        Color foreground = dark ? new Color(0xEEEEEE) : new Color(0x202124);
        Color menu = dark ? new Color(0x33363B) : new Color(0xF7F7F8);
        Color field = dark ? new Color(0x1E1F22) : Color.WHITE;
        UIManager.put("Panel.background", background);
        UIManager.put("MenuBar.background", menu);
        UIManager.put("Menu.background", menu);
        UIManager.put("Menu.foreground", foreground);
        UIManager.put("MenuItem.background", menu);
        UIManager.put("MenuItem.foreground", foreground);
        UIManager.put("MenuItem.acceleratorForeground", foreground);
        UIManager.put("CheckBoxMenuItem.background", menu);
        UIManager.put("CheckBoxMenuItem.foreground", foreground);
        UIManager.put("PopupMenu.background", menu);
        UIManager.put("Label.foreground", foreground);
        UIManager.put("Button.background", dark ? new Color(0x3A3D42) : Color.WHITE);
        UIManager.put("Button.foreground", foreground);
        UIManager.put("ToggleButton.background", dark ? new Color(0x3A3D42) : Color.WHITE);
        UIManager.put("ToggleButton.foreground", foreground);
        UIManager.put("TabbedPane.background", background);
        UIManager.put("TabbedPane.foreground", foreground);
        UIManager.put("TextField.background", field);
        UIManager.put("TextField.foreground", foreground);
        UIManager.put("TextArea.background", field);
        UIManager.put("TextArea.foreground", foreground);
        UIManager.put("ComboBox.background", field);
        UIManager.put("ComboBox.foreground", foreground);
        UIManager.put("Spinner.background", background);
        UIManager.put("OptionPane.background", background);
        UIManager.put("OptionPane.messageForeground", foreground);
        SwingUtilities.updateComponentTreeUI(this);
        getContentPane().setBackground(background);
        welcome.setBackground(background);
        toolBar.setBackground(background);
        status.setForeground(foreground);
        rebuildTools();
        repaint();
    }

    private JFileChooser chooser(String title) {
        JFileChooser chooser = new JFileChooser(preferences.lastDirectory());
        chooser.setDialogTitle(title);
        return chooser;
    }

    private void remember(JFileChooser chooser) {
        File file = chooser.getSelectedFile();
        if (file != null && file.getParent() != null) {
            preferences.setLastDirectory(file.getParent());
        }
    }

    private static JMenuItem item(String label, int key, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        if (key != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(key, KeyEvent.CTRL_DOWN_MASK));
        }
        item.addActionListener(event -> action.run());
        return item;
    }

    private static JCheckBoxMenuItem check(String label, Runnable action) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        item.addActionListener(event -> action.run());
        return item;
    }

    private static JSpinner spinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    private static JPanel form(JSpinner width, JSpinner height, String widthLabel, String heightLabel) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(widthLabel), constraints);
        constraints.gridx = 1;
        panel.add(width, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(new JLabel(heightLabel), constraints);
        constraints.gridx = 1;
        panel.add(height, constraints);
        JLabel range = new JLabel("From " + Geom.MIN_DIAGRAM + " to " + Geom.MAX_DIAGRAM + " pixels");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        panel.add(range, constraints);
        return panel;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    private static String strip(String name) {
        return name.toLowerCase().endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    /** Diagrama aberto numa aba, com o seu próprio histórico. */
    public static final class Session implements EditingSession {
        private final EditorFrame frame;
        final Diagram diagram;
        final UndoStack stack = new UndoStack();
        final DiagramCanvas canvas;
        final String title;
        File file;
        int width;
        int height;
        JLabel tabLabel;

        Session(EditorFrame frame, Diagram diagram, File file, String title, int width, int height) {
            this.frame = frame;
            this.diagram = diagram;
            this.file = file;
            this.title = title;
            this.width = width;
            this.height = height;
            this.canvas = new DiagramCanvas(diagram, this);
        }

        public EditorFrame frame() {
            return frame;
        }

        public Diagram diagram() {
            return diagram;
        }

        public UndoStack stack() {
            return stack;
        }

        public Tool tool() {
            return frame.tool();
        }

        public int modelWidth() {
            return width;
        }

        public int modelHeight() {
            return height;
        }

        public void changed() {
            canvas.refresh();
            frame.refreshChrome();
        }

        public void openProperties(Element element) {
            frame.openProperties(this, element);
        }

        @Override
        public Set<Element> selection() {
            return canvas.selection();
        }

        @Override
        public void replaceSelection(Collection<Element> elements) {
            canvas.selection().clear();
            canvas.selection().addAll(elements);
        }

        @Override
        public void clearSelection() {
            canvas.clearSelection();
        }

        @Override
        public void notifyUser(String message, boolean error) {
            frame.notify(message, error);
        }

        @Override
        public void showProperties(Element element) {
            openProperties(element);
        }

        String displayName() {
            return file == null ? title : file.getName();
        }

        String tabTitle() {
            return displayName() + (stack.isDirty() ? " *" : "");
        }
    }

    private static final class ImageSelection implements Transferable {
        private final Image image;

        private ImageSelection(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
