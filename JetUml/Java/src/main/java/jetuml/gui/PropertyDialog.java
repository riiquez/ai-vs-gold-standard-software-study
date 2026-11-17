package jetuml.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import jetuml.cmd.Commands;
import jetuml.model.Edge;
import jetuml.model.EdgeKind;
import jetuml.model.Element;
import jetuml.model.PropertySpec;
import jetuml.rules.Check;
import jetuml.rules.Rules;

/** Painel de propriedades. Tab percorre os campos e Esc fecha (RF22, RF23). */
public final class PropertyDialog extends JDialog {
    private final Element element;
    private boolean updating;

    public PropertyDialog(Window owner, EditorFrame.Session session, Element element) {
        super(owner, "Properties");
        this.element = element;
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(6, 0, 6, 8);
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        int row = 0;
        if (element.specs().isEmpty()) {
            JLabel empty = new JLabel("This element has no editable properties.");
            labelConstraints.gridy = 0;
            labelConstraints.gridwidth = 2;
            form.add(empty, labelConstraints);
        }
        for (PropertySpec spec : element.specs()) {
            labelConstraints.gridy = row;
            labelConstraints.gridwidth = 1;
            fieldConstraints.gridy = row;
            form.add(new JLabel(spec.label()), labelConstraints);
            form.add(editor(session, element, spec), fieldConstraints);
            row++;
        }
        JLabel hint = new JLabel("Ctrl+Q inserts \u00AB \u00BB. Tab moves between fields. Esc closes.");
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 0;
        hintConstraints.gridy = row;
        hintConstraints.gridwidth = 2;
        hintConstraints.insets = new Insets(10, 0, 0, 0);
        hintConstraints.anchor = GridBagConstraints.WEST;
        form.add(hint, hintConstraints);
        add(form, BorderLayout.CENTER);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    private JComponent editor(EditorFrame.Session session, Element element, PropertySpec spec) {
        return switch (spec.kind()) {
            case TEXT -> text(session, element, spec, false);
            case MULTILINE -> text(session, element, spec, true);
            case BOOLEAN -> check(session, element, spec);
            case CHOICE -> choice(session, element, spec);
        };
    }

    private JComponent text(EditorFrame.Session session, Element element, PropertySpec spec, boolean multiline) {
        if (!multiline) {
            JTextField field = new JTextField(element.get(spec.key()), 24);
            field.addActionListener(event -> apply(session, element, spec.key(), field.getText(), field));
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent event) {
                    apply(session, element, spec.key(), field.getText(), field);
                }
            });
            return field;
        }
        JTextArea area = new JTextArea(element.get(spec.key()), 5, 24);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)));
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                apply(session, element, spec.key(), area.getText(), area);
            }
        });
        return new JScrollPane(area);
    }

    private JComponent check(EditorFrame.Session session, Element element, PropertySpec spec) {
        JCheckBox box = new JCheckBox();
        box.setSelected(element.get(spec.key()).equals("true"));
        box.addActionListener(event -> apply(session, element, spec.key(), box.isSelected() ? "true" : "false", box));
        return box;
    }

    private JComponent choice(EditorFrame.Session session, Element element, PropertySpec spec) {
        JComboBox<String> combo = new JComboBox<>(spec.options().toArray(String[]::new));
        String current = element.get(spec.key());
        updating = true;
        if (current != null && !current.isBlank()) {
            combo.setSelectedItem(current);
        }
        updating = false;
        combo.addActionListener(event -> {
            if (!updating && combo.getSelectedItem() != null) {
                apply(session, element, spec.key(), combo.getSelectedItem().toString(), combo);
            }
        });
        return combo;
    }

    private void apply(EditorFrame.Session session, Element element, String key, String value, Component source) {
        if (!isDisplayable() || value == null || value.equals(element.get(key))) {
            return;
        }
        if (element instanceof Edge edge) {
            EdgeKind next = retyped(edge, key, value);
            if (next != edge.kind()) {
                Check check = Rules.canRetype(session.diagram(), edge, next);
                if (!check.ok()) {
                    session.frame().notify(check.message(), true);
                    SwingUtilities.invokeLater(() -> revert(source, element.get(key)));
                    return;
                }
            }
        }
        session.stack().push(Commands.property(session.diagram(), element, key, value));
        session.changed();
    }

    private void revert(Component source, String value) {
        updating = true;
        if (source instanceof JComboBox<?> combo) {
            combo.setSelectedItem(value);
        } else if (source instanceof JCheckBox box) {
            box.setSelected("true".equals(value));
        } else if (source instanceof JTextField field) {
            field.setText(value);
        }
        updating = false;
    }

    private static EdgeKind retyped(Edge edge, String key, String value) {
        return switch (key) {
            case "aggregationType" -> EdgeKind.fromAggregation(value);
            case "generalizationType" -> EdgeKind.fromGeneralization(value);
            case "dependencyType" -> EdgeKind.fromDependency(value);
            default -> edge.kind();
        };
    }

    public Element element() {
        return element;
    }

    public static void insertGuillemets(javax.swing.text.JTextComponent text) {
        int start = text.getSelectionStart();
        text.replaceSelection("\u00AB\u00BB");
        text.setCaretPosition(start + 1);
    }
}
