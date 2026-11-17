package jetuml.view;

import java.awt.Color;
import java.awt.Font;

public interface Draw2D {
    void line(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed);

    void polyline(int[] xs, int[] ys, Color color, float width, boolean dashed);

    void rect(int x, int y, int w, int h, Color fill, Color stroke, float width);

    void roundRect(int x, int y, int w, int h, int arc, Color fill, Color stroke, float width);

    void oval(int x, int y, int w, int h, Color fill, Color stroke, float width);

    void polygon(int[] xs, int[] ys, Color fill, Color stroke, float width);

    void text(String text, int x, int baseline, Font font, Color color);
}
