package jetuml.model;

/** Grade fixa de 10 px. O snap acontece com a grade visível ou oculta (RF36). */
public final class Geom {
    public static final int GRID = 10;
    public static final int MIN_DIAGRAM = 250;
    public static final int MAX_DIAGRAM = 4000;

    private Geom() {
    }

    public static int snap(int value) {
        return Math.round(value / (float) GRID) * GRID;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clampDiagram(int value) {
        return clamp(value, MIN_DIAGRAM, MAX_DIAGRAM);
    }
}
