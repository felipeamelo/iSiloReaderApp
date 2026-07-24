package com.dcco.app.iSilo.engine.render;

public class LayoutLine {
    public int charOffset;
    public int charLength;
    public int x;
    public int y;
    public int width;
    public int height;
    public int baseline;
    public TextStyle style;

    public LayoutLine(int charOffset, int charLength, int x, int y, int width, int height, int baseline, TextStyle style) {
        this.charOffset = charOffset;
        this.charLength = charLength;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.baseline = baseline;
        this.style = style;
    }
}
