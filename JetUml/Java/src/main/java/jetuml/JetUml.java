package jetuml;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jetuml.gui.EditorFrame;

public final class JetUml {
    private JetUml() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // o tema multiplataforma padrão continua disponível
            }
            new EditorFrame().setVisible(true);
        });
    }
}
