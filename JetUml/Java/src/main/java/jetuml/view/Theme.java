package jetuml.view;

import java.awt.Color;

public final class Theme {
    private final boolean dark;

    public Theme(boolean dark) {
        this.dark = dark;
    }

    public boolean dark() {
        return dark;
    }

    public Color canvas() {
        return dark ? new Color(0x1E1F22) : Color.WHITE;
    }

    public Color ink() {
        return dark ? new Color(0xE6E6E6) : new Color(0x1C1C1C);
    }

    public Color fill() {
        return dark ? new Color(0x2A2D31) : Color.WHITE;
    }

    public Color note() {
        return dark ? new Color(0x3D3824) : new Color(0xFFF6C8);
    }

    public Color packageFill() {
        return dark ? new Color(0x243042) : new Color(0xF3F7FC);
    }

    public Color grid() {
        return dark ? new Color(0x33373D) : new Color(0xE4E4E4);
    }

    public Color selection() {
        return dark ? new Color(0x7AA2FF) : new Color(0x1F6FEB);
    }

    public Color muted() {
        return dark ? new Color(0x9AA0A6) : new Color(0x5C6570);
    }

    public Color activation() {
        return dark ? new Color(0x3A4558) : new Color(0xF7F9FC);
    }
}
