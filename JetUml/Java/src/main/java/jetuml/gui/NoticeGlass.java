package jetuml.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Notificação temporária no canto inferior esquerdo (RF41, RF42). */
public final class NoticeGlass extends JComponent {
    private final JLabel label = new JLabel();
    private final JPanel toast = new JPanel();
    private final Timer timer;

    public NoticeGlass() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        toast.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        label.setForeground(Color.WHITE);
        toast.add(label);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.insets = new Insets(0, 16, 16, 0);
        add(toast, constraints);
        toast.setVisible(false);
        timer = new Timer(5000, event -> hideNotice());
        timer.setRepeats(false);
    }

    public void showNotice(String message, boolean error, int seconds) {
        if (seconds <= 0 || message == null || message.isBlank()) {
            return;
        }
        label.setText("<html><body style='width:280px;color:white'>" + escape(message) + "</body></html>");
        toast.setBackground(error ? new Color(0xC62828) : new Color(0x2E7D32));
        toast.setVisible(true);
        setVisible(true);
        timer.setInitialDelay(seconds * 1000);
        timer.restart();
        revalidate();
        repaint();
    }

    public void hideNotice() {
        toast.setVisible(false);
        setVisible(false);
        timer.stop();
    }

    @Override
    public boolean contains(int x, int y) {
        return toast.isVisible() && toast.getBounds().contains(x, y);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
