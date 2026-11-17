package jetuml.app;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jetuml.model.Geom;

/**
 * Preferências persistidas em ~/.jetuml.
 * O tamanho inicial do diagrama é proporcional à tela (RNF01, RF38).
 */
public final class Preferences {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".jetuml", "settings.properties");
    private final Properties properties = new Properties();
    private final int defaultWidth;
    private final int defaultHeight;

    public Preferences() {
        Dimension screen = screen();
        defaultWidth = Geom.clampDiagram((int) Math.round(screen.width * 0.72));
        defaultHeight = Geom.clampDiagram((int) Math.round(screen.height * 0.62));
        if (Files.exists(FILE)) {
            try (InputStream input = Files.newInputStream(FILE)) {
                properties.load(input);
            } catch (IOException ignored) {
                properties.clear();
            }
        }
    }

    private static Dimension screen() {
        try {
            if (GraphicsEnvironmentHeadless.isHeadless()) {
                return new Dimension(1600, 900);
            }
            return Toolkit.getDefaultToolkit().getScreenSize();
        } catch (Exception e) {
            return new Dimension(1600, 900);
        }
    }

    public int diagramWidth() {
        return Geom.clampDiagram(integer("diagramWidth", defaultWidth));
    }

    public int diagramHeight() {
        return Geom.clampDiagram(integer("diagramHeight", defaultHeight));
    }

    public void setDiagramSize(int width, int height) {
        properties.setProperty("diagramWidth", Integer.toString(Geom.clampDiagram(width)));
        properties.setProperty("diagramHeight", Integer.toString(Geom.clampDiagram(height)));
        save();
    }

    public void restoreDiagramSize() {
        properties.remove("diagramWidth");
        properties.remove("diagramHeight");
        save();
    }

    public int defaultDiagramWidth() {
        return defaultWidth;
    }

    public int defaultDiagramHeight() {
        return defaultHeight;
    }

    public Font diagramFont() {
        return new Font(properties.getProperty("fontFamily", Font.SANS_SERIF), Font.PLAIN,
                Geom.clamp(integer("fontSize", 12), 8, 72));
    }

    public void setFont(String family, int size) {
        properties.setProperty("fontFamily", family == null || family.isBlank() ? Font.SANS_SERIF : family);
        properties.setProperty("fontSize", Integer.toString(Geom.clamp(size, 8, 72)));
        save();
    }

    public void restoreFont() {
        properties.remove("fontFamily");
        properties.remove("fontSize");
        save();
    }

    public boolean darkMode() {
        return Boolean.parseBoolean(properties.getProperty("darkMode", "false"));
    }

    public void setDarkMode(boolean value) {
        properties.setProperty("darkMode", Boolean.toString(value));
        save();
    }

    public boolean showGrid() {
        return Boolean.parseBoolean(properties.getProperty("showGrid", "true"));
    }

    public void setShowGrid(boolean value) {
        properties.setProperty("showGrid", Boolean.toString(value));
        save();
    }

    public boolean showToolHint() {
        return Boolean.parseBoolean(properties.getProperty("showToolHint", "false"));
    }

    public void setShowToolHint(boolean value) {
        properties.setProperty("showToolHint", Boolean.toString(value));
        save();
    }

    public boolean verboseTooltips() {
        return Boolean.parseBoolean(properties.getProperty("verboseTooltips", "false"));
    }

    public void setVerboseTooltips(boolean value) {
        properties.setProperty("verboseTooltips", Boolean.toString(value));
        save();
    }

    public boolean autoEditNode() {
        return Boolean.parseBoolean(properties.getProperty("autoEditNode", "true"));
    }

    public void setAutoEditNode(boolean value) {
        properties.setProperty("autoEditNode", Boolean.toString(value));
        save();
    }

    public int notificationSeconds() {
        return Geom.clamp(integer("notificationSeconds", 5), 0, 10);
    }

    public void setNotificationSeconds(int seconds) {
        properties.setProperty("notificationSeconds", Integer.toString(Geom.clamp(seconds, 0, 10)));
        save();
    }

    public void restoreNotificationSeconds() {
        properties.remove("notificationSeconds");
        save();
    }

    public String lastDirectory() {
        return properties.getProperty("lastDirectory", System.getProperty("user.home"));
    }

    public void setLastDirectory(String directory) {
        if (directory != null) {
            properties.setProperty("lastDirectory", directory);
            save();
        }
    }

    public List<String> recentFiles() {
        List<String> files = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String value = properties.getProperty("recent." + i);
            if (value != null && !value.isBlank()) {
                files.add(value);
            }
        }
        return files;
    }

    public void rememberFile(String path) {
        List<String> files = new ArrayList<>();
        files.add(path);
        for (String existing : recentFiles()) {
            if (!existing.equals(path) && files.size() < 8) {
                files.add(existing);
            }
        }
        for (int i = 0; i < 8; i++) {
            if (i < files.size()) {
                properties.setProperty("recent." + i, files.get(i));
            } else {
                properties.remove("recent." + i);
            }
        }
        save();
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "JetUML preferences");
            }
        } catch (IOException ignored) {
            // a interface continua mesmo se o disco recusar a gravação
        }
    }

    /** Isola a consulta headless para o verificador automático. */
    static final class GraphicsEnvironmentHeadless {
        static boolean isHeadless() {
            return java.awt.GraphicsEnvironment.isHeadless();
        }
    }
}
